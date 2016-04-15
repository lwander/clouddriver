/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.kubernetes.api

import groovy.util.logging.Slf4j
import io.fabric8.kubernetes.api.model.ReplicationController

import java.util.concurrent.TimeUnit

@Slf4j
class KubernetesApiConsistency {
  static final int RETRY_COUNT = 20
  static final long RETRY_MAX_WAIT_MILLIS = TimeUnit.SECONDS.toMillis(10)
  static final long RETRY_INITIAL_WAIT_MILLIS = 100

  /*
   * Exponential backoff strategy for waiting on changes to replication controllers
   */
  static Boolean blockUntilReplicationControllerConsistent(KubernetesApiAdaptor apiAdaptor, ReplicationController desired) {
    def current = apiAdaptor.getReplicationController(desired.metadata.namespace, desired.metadata.name)

    def wait = RETRY_INITIAL_WAIT_MILLIS
    def attempts = 0
    while (current.status.observedGeneration < desired.status.observedGeneration) {
      attempts += 1
      if (attempts > RETRY_COUNT) {
        return false
      }

      sleep(wait)
      wait = (long) [wait * 2, RETRY_MAX_WAIT_MILLIS].min()

      current = apiAdaptor.getReplicationController(desired.metadata.namespace, desired.metadata.name)
    }

    return true
  }
}
