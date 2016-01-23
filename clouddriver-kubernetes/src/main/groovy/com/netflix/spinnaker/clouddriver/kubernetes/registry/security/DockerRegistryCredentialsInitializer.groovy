/*
 * Copyright 2015 Google, Inc.
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

package com.netflix.spinnaker.clouddriver.kubernetes.registry.security

import com.netflix.spinnaker.clouddriver.kubernetes.registry.api.docker.v2.client.DockerRegistryClient
import com.netflix.spinnaker.clouddriver.kubernetes.registry.config.DockerRegistryConfigurationProperties
import org.apache.log4j.Logger
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
@Configuration
class DockerRegistryCredentialsInitializer {
  private static final Logger log = Logger.getLogger(this.class.simpleName)

  static final DockerRegistryCredentials InitializeCredentials(DockerRegistryConfigurationProperties account) {
    try {
      def client = new DockerRegistryClient(account.address, account.email, account.username, account.password)
      print ",, testing client\n"
      print ",, ${client.auth.getToken('https://auth.docker.io/', 'token', 'registry.docker.io', 'repository:library/ubuntu:pull', true)}\n"
      return new DockerRegistryCredentials(account.repositories, client)
    } catch (e) {
      log.info "Could not connect to registry ${account.address}: ${e}"
    }
  }
}
