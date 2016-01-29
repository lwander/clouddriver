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

package com.netflix.spinnaker.clouddriver.docker.registry.api.v2.auth

import retrofit.RestAdapter
import retrofit.http.GET
import retrofit.http.Path
import retrofit.http.Query

class DockerRequestBearerToken {
  private Map<String, DockerBearerToken> fullScopeToToken
  private Map<String, BearerTokenService> realmToService

  DockerRequestBearerToken() {
    fullScopeToToken = new HashMap<String, DockerBearerToken>()
    realmToService = new HashMap<String, BearerTokenService>()
  }

  private static formatFullScope(String realm, String path, String service, String scope) {
    return "${realm}:${path}:${service}:${scope}"
  }

  private getTokenService(String realm) {
    def tokenService = realmToService.get(realm)

    if (tokenService == null) {
      def builder = new RestAdapter.Builder().setEndpoint(realm).setLogLevel(RestAdapter.LogLevel.FULL).build()
      tokenService = builder.create(BearerTokenService.class)
      realmToService[realm] = tokenService
    }

    return tokenService
  }

  public DockerBearerToken getToken(String realm, String path, String service, String scope, Boolean refresh) {
    // Refresh indicates we require a fresh token (maybe the old one has expired)
    def fullScope = formatFullScope(realm, path, service, scope)
    if (!refresh) {
      def token = fullScopeToToken.get(fullScope)

      if (token) {
        return token
      }
    }

    def tokenService = getTokenService(realm)
    def token = tokenService.getToken(path, service, scope)
    fullScopeToToken[fullScope] = token

    return token
  }

  private interface BearerTokenService {
    @GET("/{path}")
    DockerBearerToken getToken(@Path(value="path", encode=false) String path, @Query("service") String service, @Query("scope") String scope)
  }
}
