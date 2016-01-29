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

import com.netflix.spinnaker.clouddriver.docker.registry.api.v2.auth.DockerBearerTokenService
import retrofit.Callback
import retrofit.RestAdapter
import retrofit.RetrofitError
import retrofit.client.Response
import retrofit.http.GET
import retrofit.http.Headers
import retrofit.http.Path

class DockerRegistryClient {
  private DockerBearerTokenService tokenService

  private String address
  private String email
  private String username
  private String password
  private DockerRegistryService registryService
  private Map<String, DockerRegistryTags> tags

  DockerRegistryClient(String address, String email, String username, String password) {
    this.address = address
    this.email = email
    this.username = username
    this.password = password
    this.tokenService = new DockerBearerTokenService()
    this.registryService = new RestAdapter.Builder().setEndpoint(address).setLogLevel(RestAdapter.LogLevel.FULL).build().create(DockerRegistryService)
  }

  interface DockerRegistryService {
    @GET("/v2/{repository}/tags/list")
    @Headers("User-Agent: Spinnaker-Clouddriver")
    void getTags(@Path(value="repository", encode=false) String repository, Callback<DockerRegistryTags> callback)
  }

  public void requestTags(String repository) {
    print ",, $repository"
    registryService.getTags(repository, RequestResource.Request(tags, repository))
  }

  private static class RequestResource<T> {
    static Callback<T> Request(Map<String, T> store, String key) {
      return new Callback<T>() {
        @Override
        void success(T t, Response response) {
          print ",, response $response"
        }

        @Override
        void failure(RetrofitError error) {
          if (error.response.status == 401) {
          }
        }
      }
    }
  }

}
