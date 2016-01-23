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

package com.netflix.spinnaker.clouddriver.kubernetes.registry.api.docker.v2.client

import com.netflix.spinnaker.clouddriver.kubernetes.registry.api.docker.v2.auth.DockerRequestBearerToken
import retrofit.http.GET
import retrofit.http.Path

class DockerRegistryClient {
  private DockerRequestBearerToken tokenService

  private String address
  private String email
  private String username
  private String password

  DockerRegistryClient(String address, String email, String username, String password) {
    this.address = address
    this.email = email
    this.username = username
    this.password = password
    this.tokenService = new DockerRequestBearerToken()
  }

  interface DockerRegistryService {
    @GET("/v2/{repository}/tags/list")
    DockerRegistryTags getTags(@Path(value="repository", encode=false) String repository)
  }
}
