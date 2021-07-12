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

package de.codecentric.spring.boot.chaos.monkey.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.codecentric.spring.boot.chaos.monkey.endpoints.AssaultPropertiesUpdate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

/** @author Benjamin Wilms */
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "chaos.monkey.assaults")
@Validated
@AssaultPropertiesLatencyRangeConstraint
public class AssaultProperties {
  @Min(value = 1)
  @Max(value = 10000)
  private int level = 1;

  @Min(value = 1)
  @Max(value = Integer.MAX_VALUE)
  private int latencyRangeStart = 1000;

  @Min(value = 1)
  @Max(value = Integer.MAX_VALUE)
  private int latencyRangeEnd = 3000;

  private boolean latencyActive = false;

  private boolean exceptionsActive = false;

  @AssaultExceptionConstraint private AssaultException exception;

  private boolean killApplicationActive = false;

  private volatile boolean memoryActive = false;

  @Min(value = 1500)
  @Max(value = Integer.MAX_VALUE)
  private int memoryMillisecondsHoldFilledMemory = 90000;

  @Min(value = 100)
  @Max(value = 30000)
  private int memoryMillisecondsWaitNextIncrease = 1000;

  @DecimalMax("1.0")
  @DecimalMin("0.01")
  private double memoryFillIncrementFraction = 0.15;

  @DecimalMax("1.0")
  @DecimalMin("0.01")
  private double memoryFillTargetFraction = 0.25;

  private volatile boolean cpuActive = false;

  @Min(value = 1500)
  @Max(value = Integer.MAX_VALUE)
  private int cpuMillisecondsHoldLoad = 90000;

  @DecimalMax("1.0")
  @DecimalMin("0.01")
  private double cpuLoadTargetFraction = 0.9;

  @Value("${chaos.monkey.assaults.runtime.scope.assault.cron.expression:OFF}")
  private String runtimeAssaultCronExpression;

  private List<String> watchedCustomServices;

  public AssaultException getException() {
    return exception == null ? new AssaultException() : exception;
  }

  public void setException(AssaultException exception) {
    this.exception = exception;
  }

  @JsonIgnore
  public int getTroubleRandom() {
    return ThreadLocalRandom.current().nextInt(1, getLevel() + 1);
  }

  @JsonIgnore
  public int chooseAssault(int amount) {
    return ThreadLocalRandom.current().nextInt(0, amount);
  }

  @JsonIgnore
  public boolean isWatchedCustomServicesActive() {
    return !CollectionUtils.isEmpty(watchedCustomServices);
  }

  public AssaultPropertiesUpdate toDto() {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(this, AssaultPropertiesUpdate.class);
  }
}
