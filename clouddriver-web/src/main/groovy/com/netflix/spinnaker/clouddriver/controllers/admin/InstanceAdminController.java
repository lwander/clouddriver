/*
 * Copyright 2018 Google, Inc.
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
 *
 */

package com.netflix.spinnaker.clouddriver.controllers.admin;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/admin/instance")
@ConditionalOnProperty("admin.instance.enabled")
@Slf4j
public class InstanceAdminController {
  @Bean
  public ConnectorCustomizer gracefulShutdown() {
    return new ConnectorCustomizer();
  }

  @Autowired
  ConnectorCustomizer connectorCustomizer;

  @RequestMapping(value = "/disable", method = RequestMethod.POST)
  void disableInstance() {
    log.warn("Instance being disabled, will no longer accept requests...");
    connectorCustomizer.disconnect();
  }

  @Bean
  public EmbeddedServletContainerCustomizer tomcatCustomizer(ConnectorCustomizer customizer) {
    return container -> {
      if (container instanceof TomcatEmbeddedServletContainerFactory) {
        ((TomcatEmbeddedServletContainerFactory) container).addConnectorCustomizers(customizer);
      }
    };
  }

  @Slf4j
  private static class ConnectorCustomizer implements TomcatConnectorCustomizer {

    private volatile Connector connector;

    @Override
    public void customize(Connector connector) {
      this.connector = connector;
    }

    public void disconnect() {
      this.connector.pause();
      Executor executor = this.connector.getProtocolHandler().getExecutor();
      if (executor instanceof ThreadPoolExecutor) {
        ((ThreadPoolExecutor) executor).shutdown();
      }
    }

  }
}
