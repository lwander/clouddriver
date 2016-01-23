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

package com.netflix.spinnaker.clouddriver.kubernetes.registry.security;

import com.netflix.spinnaker.clouddriver.kubernetes.registry.api.docker.v2.client.DockerRegistryClient;

public class DockerRegistryCredentials {
  private final DockerRegistryClient client;
  private final List<String> repositories;

  public DockerRegistryCredentials(List<String> repositories, DockerRegistryClient client) {
    this.client = client;
    this.repositories = repositories;
  }

  public DockerRegistryClient getClient() {
    return client;
  }

  public List<String> getRepositories() {
    return repositories;
  }
}
