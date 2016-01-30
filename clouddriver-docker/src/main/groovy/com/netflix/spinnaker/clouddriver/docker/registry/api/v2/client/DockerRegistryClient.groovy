/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.docker.registry.api.v2.client

import com.google.gson.GsonBuilder
import com.netflix.spinnaker.clouddriver.docker.registry.api.v2.auth.DockerBearerTokenService
import retrofit.Callback
import retrofit.RestAdapter
import retrofit.RetrofitError
import retrofit.client.Response
import retrofit.converter.GsonConverter
import retrofit.http.GET
import retrofit.http.Header
import retrofit.http.Headers
import retrofit.http.Path

class DockerRegistryClient {
  private DockerBearerTokenService tokenService

  private DockerRegistryService registryService
  // Temporary replacement for Redis caching
  private Map<String, DockerRegistryTags> tagsStore
  private GsonConverter converter
  private RequestResource requester

  DockerRegistryClient(String address, String email, String username, String password) {
    this.tokenService = new DockerBearerTokenService(username, password)
    this.registryService = new RestAdapter.Builder().setEndpoint(address).setLogLevel(RestAdapter.LogLevel.FULL).build().create(DockerRegistryService)
    this.requester = new RequestResource()
    this.converter = new GsonConverter(new GsonBuilder().create())
    this.tagsStore = new HashMap()
  }

  interface DockerRegistryService {
    @GET("/{repository}/tags/list")
    @Headers([
      "User-Agent: Spinnaker-Clouddriver",
      "Docker-Distribution-API-Version: registry/2.0"
    ]) // TODO(lwander) get clouddriver version #
    DockerRegistryTags getTags(@Path(value="repository", encode=false) String repository, @Header("Authorization") String token)

    @GET("/{repository}/tags/list")
    @Headers([
      "User-Agent: Spinnaker-Clouddriver",
      "Docker-Distribution-API-Version: registry/2.0"
    ]) // TODO(lwander) get clouddriver version #
    void getTags(@Path(value="repository", encode=false) String repository, Callback<DockerRegistryTags> callback)

    @GET("/")
    @Headers([
      "User-Agent: Spinnaker-Clouddriver",
      "Docker-Distribution-API-Version: registry/2.0"
    ]) // TODO(lwander) get clouddriver version #
    Response checkVersion()
  }

  /*
   * Implements token request flow described here https://docs.docker.com/registry/spec/auth/token/
   */
  public void requestTags(String repository) {
    registryService.getTags(repository, requester.Request({DockerRegistryTags result -> tagsStore[repository] = result}, {String token -> registryService.getTags(repository, "Bearer $token")}))
  }

  public Response checkVersion() {
    registryService.checkVersion()
  }

  private class RequestResource<T> {
    Callback<T> Request(Closure store, Closure<T> retry) {
      return new Callback<T>() {
        @Override
        void success(T t, Response response) {
          T convertedResult = (T) converter.fromBody(response.body, T)
          store(convertedResult)
        }

        @Override
        void failure(RetrofitError error) {
          if (error.response.status == 401) {
            def tokenResponse = tokenService.getToken(error.response.headers)
            def token = tokenResponse.bearer_token ?: tokenResponse.token
            store(retry(token))
          } else {
            throw error
          }
        }
      }
    }
  }
}
