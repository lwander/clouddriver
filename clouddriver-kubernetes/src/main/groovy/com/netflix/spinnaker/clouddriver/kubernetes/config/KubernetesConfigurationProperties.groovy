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

package com.netflix.spinnaker.clouddriver.kubernetes.config

import com.netflix.spinnaker.clouddriver.kubernetes.registry.config.DockerRegistryConfigurationProperties
import groovy.transform.ToString

@ToString(includeNames = true)
class KubernetesConfigurationProperties {
  @ToString(includeNames = true)
  static class ManagedAccount {
    String name
    String environment
    String accountType
    // Kubernetes endpoint address.
    String master
    // Username required for authenticating with Kubernetes endpoint.
    String username
    // Password required for authenticating with Kubernetes endpoint.
    String password
    // (Default = ['default']) Namespaces to be managed.
    List<String> namespaces
    // Credentials for authenticating with Docker Registry (must conform to v2 API).
    List<DockerRegistryConfigurationProperties> registries
  }

  List<ManagedAccount> accounts = []
}

