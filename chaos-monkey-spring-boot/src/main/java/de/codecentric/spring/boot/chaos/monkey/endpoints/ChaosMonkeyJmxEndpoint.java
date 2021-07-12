/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.codecentric.spring.boot.chaos.monkey.endpoints;

import de.codecentric.spring.boot.chaos.monkey.configuration.ChaosMonkeySettings;
import de.codecentric.spring.boot.chaos.monkey.configuration.WatcherProperties;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpoint;

/** @author Benjamin Wilms */
@JmxEndpoint(enableByDefault = false, id = "chaosmonkeyjmx")
public class ChaosMonkeyJmxEndpoint {

  private final ChaosMonkeySettings chaosMonkeySettings;

  public ChaosMonkeyJmxEndpoint(ChaosMonkeySettings chaosMonkeySettings) {
    this.chaosMonkeySettings = chaosMonkeySettings;
  }

  @ReadOperation
  public AssaultPropertiesUpdate getAssaultProperties() {
    return chaosMonkeySettings.getAssaultProperties().toDto();
  }

  @WriteOperation
  public String toggleLatencyAssault() {
    this.chaosMonkeySettings
        .getAssaultProperties()
        .setLatencyActive(!this.getAssaultProperties().getLatencyActive());
    return String.valueOf(this.getAssaultProperties().getLatencyActive());
  }

  @WriteOperation
  public String toggleExceptionAssault() {
    this.chaosMonkeySettings
        .getAssaultProperties()
        .setExceptionsActive(!this.getAssaultProperties().getExceptionsActive());
    return String.valueOf(this.getAssaultProperties().getExceptionsActive());
  }

  @WriteOperation
  public String toggleKillApplicationAssault() {
    this.chaosMonkeySettings
        .getAssaultProperties()
        .setKillApplicationActive(!this.getAssaultProperties().getKillApplicationActive());
    return String.valueOf(this.getAssaultProperties().getKillApplicationActive());
  }

  @WriteOperation
  public String toggleCpuAssault() {
    this.chaosMonkeySettings
        .getAssaultProperties()
        .setCpuActive(!this.getAssaultProperties().getCpuActive());
    return String.valueOf(this.getAssaultProperties().getCpuActive());
  }

  @ReadOperation()
  public String isChaosMonkeyActive() {
    return String.valueOf(this.chaosMonkeySettings.getChaosMonkeyProperties().isEnabled());
  }

  @WriteOperation
  public String enableChaosMonkey() {
    this.chaosMonkeySettings.getChaosMonkeyProperties().setEnabled(true);
    return "Chaos Monkey is enabled";
  }

  @WriteOperation
  public String disableChaosMonkey() {
    this.chaosMonkeySettings.getChaosMonkeyProperties().setEnabled(false);
    return "Chaos Monkey is disabled";
  }

  @ReadOperation
  public WatcherProperties getWatcherProperties() {
    return this.chaosMonkeySettings.getWatcherProperties();
  }
}
