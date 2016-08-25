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

package com.netflix.spinnaker.clouddriver.kubernetes.provider.agent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.*
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport
import com.netflix.spinnaker.clouddriver.kubernetes.KubernetesCloudProvider
import com.netflix.spinnaker.clouddriver.kubernetes.cache.Keys
import com.netflix.spinnaker.clouddriver.kubernetes.provider.KubernetesProvider
import com.netflix.spinnaker.clouddriver.kubernetes.provider.view.MutableCacheData
import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesCredentials
import groovy.util.logging.Slf4j
import io.fabric8.kubernetes.api.model.Pod

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE

@Slf4j
class KubernetesInstanceCachingAgent implements  CachingAgent, OnDemandAgent, AccountAware {
  static final Set<AgentDataType> types = Collections.unmodifiableSet([
      AUTHORITATIVE.forType(Keys.Namespace.INSTANCES.ns),
  ] as Set)

  final KubernetesCloudProvider kubernetesCloudProvider
  final String accountName
  final String namespace
  final KubernetesCredentials credentials
  final ObjectMapper objectMapper
  final Registry registry
  final OnDemandMetricsSupport metricsSupport

  KubernetesInstanceCachingAgent(KubernetesCloudProvider kubernetesCloudProvider,
                                 String accountName,
                                 KubernetesCredentials credentials,
                                 String namespace,
                                 ObjectMapper objectMapper,
                                 Registry registry) {
    this.kubernetesCloudProvider = kubernetesCloudProvider
    this.accountName = accountName
    this.credentials = credentials
    this.objectMapper = objectMapper
    this.namespace = namespace
    this.registry = registry
    this.metricsSupport = new OnDemandMetricsSupport(registry, this, "$kubernetesCloudProvider.id:$OnDemandAgent.OnDemandType.Instance")
  }

  @Override
  String getAccountName() {
    return accountName
  }

  @Override
  Collection<AgentDataType> getProvidedDataTypes() {
    return types
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    Long start = System.currentTimeMillis()
    List<Pod> pods = credentials.apiAdaptor.getPods(namespace)

    def evictFromOnDemand = []
    def keepInOnDemand = []

    providerCache.getAll(Keys.Namespace.ON_DEMAND.ns,
      pods.collect { Keys.getInstanceKey(accountName, namespace, it.metadata.name) }).each {
      // Ensure that we don't overwrite data that was inserted by the `handle` method while we retrieved the
      // replication controllers. Furthermore, cache data that hasn't been processed needs to be updated in the ON_DEMAND
      // cache, so don't evict data without a processedCount > 0.
      if (it.attributes.cacheTime < start && it.attributes.processedCount > 0) {
        evictFromOnDemand << it
      } else {
        keepInOnDemand << it
      }
    }

    def result = buildCacheResult(pods, keepInOnDemand.collectEntries { [(it.id): it] }, evictFromOnDemand*.id, start)

    result.cacheResults[Keys.Namespace.ON_DEMAND.ns].each {
      it.attributes.processedTime = System.currentTimeMillis()
      it.attributes.processedCount = (it.attributes.processedCount ?: 0) + 1
    }

    return result
  }

  private static void cache(Map<String, List<CacheData>> cacheResults, String namespace, Map<String, CacheData> cacheDataById) {
    cacheResults[namespace].each {
      def existingCacheData = cacheDataById[it.id]
      if (!existingCacheData) {
        cacheDataById[it.id] = it
      } else {
        existingCacheData.attributes.putAll(it.attributes)
        it.relationships.each { String relationshipName, Collection<String> relationships ->
          existingCacheData.relationships[relationshipName].addAll(relationships)
        }
      }
    }
  }

  private CacheResult buildCacheResult(List<Pod> pods, Map<String, CacheData> onDemandKeep, List<String> onDemandEvict, Long start) {
    log.info("Describing items in ${agentType}")

    Map<String, MutableCacheData> cachedInstances = MutableCacheData.mutableCacheMap()

    for (Pod pod : pods) {
      if (!pod) {
        continue
      }

      def onDemandData = onDemandKeep ? onDemandKeep[Keys.getInstanceKey(accountName, namespace, pod.metadata.name)] : null

      if (onDemandData && onDemandData.attributes.cachetime >= start) {
        Map<String, List<CacheData>> cacheResults = objectMapper.readValue(onDemandData.attributes.cacheResults as String,
            new TypeReference<Map<String, List<MutableCacheData>>>() { })
        cache(cacheResults, Keys.Namespace.INSTANCES.ns, cachedInstances)
      } else {
        def key = Keys.getInstanceKey(accountName, namespace, pod.metadata.name)
        cachedInstances[key].with {
          attributes.name = pod.metadata.name
          attributes.pod = pod
        }

      }
    }

    log.info("Caching ${cachedInstances.size()} instances in ${agentType}")

    new DefaultCacheResult([
      (Keys.Namespace.INSTANCES.ns): cachedInstances.values(),
    ], [:])
  }

  @Override
  String getAgentType() {
    "${accountName}/${namespace}/${KubernetesInstanceCachingAgent.simpleName}"
  }

  @Override
  String getProviderName() {
    KubernetesProvider.PROVIDER_NAME
  }

  @Override
  String getOnDemandAgentType() {
    "${getAgentType()}-OnDemand"
  }

  @Override
  OnDemandMetricsSupport getMetricsSupport() {
    return metricsSupport
  }

  @Override
  boolean handles(OnDemandAgent.OnDemandType type, String cloudProvider) {
    return type == OnDemandAgent.OnDemandType.Instance && cloudProvider == getProviderName()
  }

  @Override
  OnDemandAgent.OnDemandResult handle(ProviderCache providerCache, Map<String, ? extends Object> data) {
    if (!data.containsKey("instanceName")) {
      return null
    }

    if (data.account != accountName) {
      return null
    }

    if (data.region != namespace) {
      return null
    }

    def instanceName = data.instanceName.toString()

    Pod pod = metricsSupport.readData {
      credentials.apiAdaptor.getPod(namespace, instanceName)
    }

    CacheResult result = metricsSupport.transformData {
      buildCacheResult([pod], [:], [], Long.MAX_VALUE)
    }

    def jsonResult = objectMapper.writeValueAsString(result.cacheResults)

    if (result.cacheResults.values().flatten().isEmpty()) {
      // Avoid writing an empty onDemand cache record (instead delete any that may have previously existed).
      providerCache.evictDeletedItems(Keys.Namespace.ON_DEMAND.ns, [Keys.getInstanceKey(accountName, namespace, instanceName)])
    } else {
      metricsSupport.onDemandStore {
        def cacheData = new DefaultCacheData(
          Keys.getInstanceKey(accountName, namespace, instanceName),
          10 * 60, // ttl is 10 minutes
          [
            cacheTime: System.currentTimeMillis(),
            cacheResults: jsonResult,
            processedCount: 0,
            processedTime: null
          ],
          [:]
        )

        providerCache.putCacheData(Keys.Namespace.ON_DEMAND.ns, cacheData)
      }
    }

    // Evict this server group if it no longer exists.
    Map<String, Collection<String>> evictions = pod ? [:] : [
      (Keys.Namespace.INSTANCES.ns): [
        Keys.getInstanceKey(accountName, namespace, instanceName)
      ]
    ]

    log.info("On demand cache refresh (data: ${data}) succeeded.")

    return new OnDemandAgent.OnDemandResult(
      sourceAgentType: getOnDemandAgentType(),
      cacheResult: result,
      evictions: evictions
    )
  }

  @Override
  Collection<Map> pendingOnDemandRequests(ProviderCache providerCache) {
    def keys = providerCache.getIdentifiers(Keys.Namespace.ON_DEMAND.ns)
    keys = keys.findResults {
      def parse = Keys.parse(it)
      if (parse && parse.namespace == namespace && parse.account == accountName) {
        return it
      } else {
        return null
      }
    }

    providerCache.getAll(Keys.Namespace.ON_DEMAND.ns, keys).collect {
      [
        details  : Keys.parse(it.id),
        cacheTime: it.attributes.cacheTime,
        processedCount: it.attributes.processedCount,
        processedTime: it.attributes.processedTime
      ]
    }
  }
}
