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

package com.netflix.spinnaker.clouddriver.kubernetes.api;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

import java.util.List;

public class KubernetesApiAdaptor {
  static public String LOAD_BALANCER_LABEL_PREFIX = "load-balancer-";
  static public String REPLICATION_CONTROLLER_LABEL = "replication-controller";
  static public String JOB_LABEL = "job";
  KubernetesClient client;

  public KubernetesApiAdaptor(KubernetesClient client) {
    if (client == null) {
      throw new IllegalArgumentException("Client may not be null.");
    }
    this.client = client;
  }

  public Ingress createIngress(String namespace, Ingress ingress) {
    try {
      return client.extensions().ingress().inNamespace(namespace).create(ingress);
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Create Ingress", e);
    }
  }

  public Ingress replaceIngress(String namespace, String name, Ingress ingress) {
    try {
      return client.extensions().ingress().inNamespace(namespace).withName(name).replace(ingress);
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Replace Ingress", e);
    }
  }

  public Ingress getIngress(String namespace, String name) {
    try {
      return client.extensions().ingress().inNamespace(namespace).withName(name).get();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Ingress", e);
    }
  }

  public boolean deleteIngress(String namespace, String name) {
    try {
      return client.extensions().ingress().inNamespace(namespace).withName(name).delete();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Delete Ingress", e);
    }
  }

  public List<Ingress> getIngresses(String namespace) {
    try {
      return client.extensions().ingress().inNamespace(namespace).list().getItems();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Ingresses", e);
    }
  }

  public List<ReplicationController> getReplicationControllers(String namespace) {
    try {
      return client.replicationControllers().inNamespace(namespace).list().getItems();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Replication Controllers", e);
    }
  }

  public List<Pod> getReplicationControllerPods(String namespace, String replicationControllerName) {
    try {
      return client.pods().inNamespace(namespace).withLabel(REPLICATION_CONTROLLER_LABEL, replicationControllerName).list().getItems();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Replication Controller Pods", e);
    }
  }

  public List<Pod> getJobPods(String namespace, String jobName) {
    try {
      return client.pods().inNamespace(namespace).withLabel(JOB_LABEL, jobName).list().getItems();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Job Pods", e);
    }
  }

  public Pod getPod(String namespace, String name) {
    try {
      return client.pods().inNamespace(namespace).withName(name).get();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Pod", e);
    }
  }

  public boolean deletePod(String namespace, String name) {
    try {
      return client.pods().inNamespace(namespace).withName(name).delete();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Delete Pod", e);
    }
  }

  public List<Pod> getPods(String namespace) {
    try {
      return client.pods().inNamespace(namespace).list().getItems();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Pods", e);
    }
  }

  public ReplicationController getReplicationController(String namespace, String serverGroupName) {
    try {
      return client.replicationControllers().inNamespace(namespace).withName(serverGroupName).get();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Replication Controller", e);
    }
  }

  public ReplicationController createReplicationController(String namespace, ReplicationController replicationController) {
    try {
      return client.replicationControllers().inNamespace(namespace).create(replicationController);
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Create Replication Controller", e);
    }
  }

  public ReplicationController resizeReplicationController(String namespace, String name, int size) {
    try {
      return client.replicationControllers().inNamespace(namespace).withName(name).scale(size);
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Resize Replication Controller", e);
    }
  }

  public boolean hardDestroyReplicationController(String namespace, String name) {
    try {
      return client.replicationControllers().inNamespace(namespace).withName(name).delete();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Hard Destroy Replication Controller", e);
    }
  }

  public void togglePodLabels(String namespace, String name, List<String> keys, String value) {
    try {
      PodFluent.MetadataNested edit = client.pods().inNamespace(namespace).withName(name).edit().editMetadata();

      for (String key : keys) {
        edit.removeFromLabels(key);
        edit.addToLabels(key, value);
      }

      ((DoneablePod) edit.endMetadata()).done();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Toggle Pod Labels", e);
    }
  }

  public ReplicationController toggleReplicationControllerSpecLabels(String namespace, String name, List<String> keys, String value) {
    try {
      PodTemplateSpecFluent.MetadataNested edit = client.replicationControllers()
          .inNamespace(namespace)
          .withName(name)
          .cascading(false)
          .edit()
          .editSpec()
          .editTemplate()
          .editMetadata();

      for (String key : keys) {
        edit.removeFromLabels(key);
        edit.addToLabels(key, value);
      }

      ReplicationControllerSpecFluentImpl.TemplateNestedImpl rc = (ReplicationControllerSpecFluentImpl.TemplateNestedImpl) edit.endMetadata();
      ReplicationControllerFluent.SpecNested rcS = (ReplicationControllerFluent.SpecNested) rc.endTemplate();
      return ((DoneableReplicationController) rcS.endSpec()).done();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Toggle Replication Controller Labels", e);
    }
  }

  public Service getService(String namespace, String service) {
    try {
      return client.services().inNamespace(namespace).withName(service).get();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Service", e);
    }
  }

  public Service createService(String namespace, Service service) {
    try {
      return client.services().inNamespace(namespace).create(service);
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Create Service", e);
    }
  }

  public boolean deleteService(String namespace, String name) {
    try {
      return client.services().inNamespace(namespace).withName(name).delete();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Delete Service", e);
    }
  }

  public List<Service> getServices(String namespace) {
    try {
      return client.services().inNamespace(namespace).list().getItems();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Services", e);
    }
  }

  public Service replaceService(String namespace, String name, Service service) {
    try {
      return client.services().inNamespace(namespace).withName(name).replace(service);
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Replace Service", e);
    }
  }

  public Secret getSecret(String namespace, String secret) {
    try {
      return client.secrets().inNamespace(namespace).withName(secret).get();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Secret", e);
    }
  }

  public Boolean deleteSecret(String namespace, String secret) {
    try {
      return client.secrets().inNamespace(namespace).withName(secret).delete();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Delete Secret", e);
    }
  }

  public Secret createSecret(String namespace, Secret secret) {
    try {
      return client.secrets().inNamespace(namespace).create(secret);
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Create Secret", e);
    }
  }

  public Namespace getNamespace(String namespace) {
    try {
      return client.namespaces().withName(namespace).get();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Namespace", e);
    }
  }

  public Namespace createNamespace(Namespace namespace) {
    try {
      return client.namespaces().create(namespace);
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Create Namespace", e);
    }
  }

  public Job createJob(String namespace, Job job) {
    try {
      return client.extensions().jobs().inNamespace(namespace).create(job);
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Create Job", e);
    }
  }

  public List<Job> getJobs(String namespace) {
    try {
      return client.extensions().jobs().inNamespace(namespace).list().getItems();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Jobs", e);
    }
  }

  public Job getJob(String namespace, String name) {
    try {
      return client.extensions().jobs().inNamespace(namespace).withName(name).get();
    } catch (KubernetesClientException e) {
      throw new KubernetesOperationException("Get Job", e);
    }
  }
}
