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
 *
 */

package de.codecentric.spring.boot.chaos.monkey.configuration;

import com.sun.management.OperatingSystemMXBean;
import de.codecentric.spring.boot.chaos.monkey.assaults.*;
import de.codecentric.spring.boot.chaos.monkey.component.*;
import de.codecentric.spring.boot.chaos.monkey.configuration.toggles.ChaosToggleNameMapper;
import de.codecentric.spring.boot.chaos.monkey.configuration.toggles.ChaosToggles;
import de.codecentric.spring.boot.chaos.monkey.configuration.toggles.DefaultChaosToggleNameMapper;
import de.codecentric.spring.boot.chaos.monkey.configuration.toggles.DefaultChaosToggles;
import de.codecentric.spring.boot.chaos.monkey.endpoints.ChaosMonkeyJmxEndpoint;
import de.codecentric.spring.boot.chaos.monkey.endpoints.ChaosMonkeyRestEndpoint;
import de.codecentric.spring.boot.chaos.monkey.watcher.aspect.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.util.StreamUtils;

/** @author Benjamin Wilms */
@Configuration
@Profile("chaos-monkey")
@EnableConfigurationProperties({
  ChaosMonkeyProperties.class,
  AssaultProperties.class,
  WatcherProperties.class
})
@Import({
  UnleashChaosConfiguration.class,
  ChaosMonkeyWebClientConfiguration.class,
  ChaosMonkeyRestTemplateConfiguration.class
})
@EnableScheduling
public class ChaosMonkeyConfiguration {

  private static final Logger Logger = LoggerFactory.getLogger(ChaosMonkeyConfiguration.class);

  private static final String CHAOS_MONKEY_TASK_SCHEDULER = "chaosMonkeyTaskScheduler";

  private final ChaosMonkeyProperties chaosMonkeyProperties;

  private final WatcherProperties watcherProperties;

  private final AssaultProperties assaultProperties;

  public ChaosMonkeyConfiguration(
      ChaosMonkeyProperties chaosMonkeyProperties,
      WatcherProperties watcherProperties,
      AssaultProperties assaultProperties) {
    this.chaosMonkeyProperties = chaosMonkeyProperties;
    this.watcherProperties = watcherProperties;
    this.assaultProperties = assaultProperties;

    try {
      String chaosLogo =
          StreamUtils.copyToString(
              new ClassPathResource("chaos-logo.txt").getInputStream(), Charset.defaultCharset());
      Logger.info(chaosLogo);
    } catch (IOException e) {
      Logger.info("Chaos Monkey - ready to do evil");
    }
  }

  @Bean
  @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
  public Metrics metrics() {
    return new Metrics();
  }

  @Bean
  public MetricEventPublisher publisher() {
    return new MetricEventPublisher();
  }

  @Bean
  public ChaosMonkeySettings settings() {
    return new ChaosMonkeySettings(chaosMonkeyProperties, assaultProperties, watcherProperties);
  }

  @Bean
  public LatencyAssault latencyAssault() {
    return new LatencyAssault(settings(), publisher());
  }

  @Bean
  public ExceptionAssault exceptionAssault() {
    return new ExceptionAssault(settings(), publisher());
  }

  @Bean
  public KillAppAssault killAppAssault() {
    return new KillAppAssault(settings(), publisher());
  }

  @Bean
  public MemoryAssault memoryAssault() {
    return new MemoryAssault(Runtime.getRuntime(), settings(), publisher());
  }

  @Bean
  public CpuAssault cpuAssault() {
    return new CpuAssault(
        ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class), settings(), publisher());
  }

  @Bean
  public ChaosMonkeyRequestScope chaosMonkeyRequestScope(
      List<ChaosMonkeyRequestAssault> chaosMonkeyAssaults,
      List<ChaosMonkeyAssault> allAssaults,
      ChaosToggles chaosToggles,
      ChaosToggleNameMapper chaosToggleNameMapper) {
    return new ChaosMonkeyRequestScope(
        settings(),
        chaosMonkeyAssaults,
        allAssaults,
        publisher(),
        chaosToggles,
        chaosToggleNameMapper);
  }

  @Bean
  @ConditionalOnMissingBean(ChaosToggleNameMapper.class)
  public ChaosToggleNameMapper chaosToggleNameMapper(ChaosMonkeyProperties chaosMonkeyProperties) {
    return new DefaultChaosToggleNameMapper(chaosMonkeyProperties.getTogglePrefix());
  }

  @Bean
  @ConditionalOnMissingBean(ChaosToggles.class)
  public ChaosToggles chaosToggles() {
    return new DefaultChaosToggles();
  }

  @Bean
  public ChaosMonkeyScheduler scheduler(
      @Qualifier(CHAOS_MONKEY_TASK_SCHEDULER) TaskScheduler scheduler,
      ChaosMonkeyRuntimeScope runtimeScope) {
    ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
    registrar.setTaskScheduler(scheduler);
    return new ChaosMonkeyScheduler(registrar, assaultProperties, runtimeScope);
  }

  @Bean(name = CHAOS_MONKEY_TASK_SCHEDULER)
  public TaskScheduler chaosMonkeyTaskScheduler() {
    return new ThreadPoolTaskScheduler();
  }

  @Bean
  public ChaosMonkeyRuntimeScope chaosMonkeyRuntimeScope(
      List<ChaosMonkeyRuntimeAssault> chaosMonkeyAssaults) {
    return new ChaosMonkeyRuntimeScope(settings(), chaosMonkeyAssaults);
  }

  @Bean
  @DependsOn("chaosMonkeyRequestScope")
  public SpringControllerAspect controllerAspect(ChaosMonkeyRequestScope chaosMonkeyRequestScope) {
    return new SpringControllerAspect(chaosMonkeyRequestScope, publisher(), watcherProperties);
  }

  @Bean
  @DependsOn("chaosMonkeyRequestScope")
  public SpringRestControllerAspect restControllerAspect(
      ChaosMonkeyRequestScope chaosMonkeyRequestScope) {
    return new SpringRestControllerAspect(chaosMonkeyRequestScope, publisher(), watcherProperties);
  }

  @Bean
  @DependsOn("chaosMonkeyRequestScope")
  public SpringServiceAspect serviceAspect(ChaosMonkeyRequestScope chaosMonkeyRequestScope) {
    return new SpringServiceAspect(chaosMonkeyRequestScope, publisher(), watcherProperties);
  }

  @Bean
  @DependsOn("chaosMonkeyRequestScope")
  public SpringComponentAspect componentAspect(ChaosMonkeyRequestScope chaosMonkeyRequestScope) {
    return new SpringComponentAspect(chaosMonkeyRequestScope, publisher(), watcherProperties);
  }

  @Bean
  @DependsOn("chaosMonkeyRequestScope")
  @ConditionalOnClass(name = "org.springframework.data.repository.Repository")
  // Creates aspects that match interfaces annotated with @Repository
  public SpringRepositoryAspectJPA repositoryAspectJpa(
      ChaosMonkeyRequestScope chaosMonkeyRequestScope) {
    return new SpringRepositoryAspectJPA(chaosMonkeyRequestScope, publisher(), watcherProperties);
  }

  @Bean
  @DependsOn("chaosMonkeyRequestScope")
  // creates aspects that match simple classes annotated with @repository
  public SpringRepositoryAspectJDBC repositoryAspectJdbc(
      ChaosMonkeyRequestScope chaosMonkeyRequestScope) {
    return new SpringRepositoryAspectJDBC(chaosMonkeyRequestScope, publisher(), watcherProperties);
  }

  @Bean
  @DependsOn("chaosMonkeyRequestScope")
  @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
  public SpringBootHealthIndicatorAspect springBootHealthIndicatorAspect(
      ChaosMonkeyRequestScope chaosMonkeyRequestScope) {
    return new SpringBootHealthIndicatorAspect(chaosMonkeyRequestScope);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnAvailableEndpoint
  public ChaosMonkeyRestEndpoint chaosMonkeyRestEndpoint(
      ChaosMonkeyRuntimeScope runtimeScope, ChaosMonkeyScheduler scheduler) {
    return new ChaosMonkeyRestEndpoint(settings(), runtimeScope, scheduler);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnAvailableEndpoint
  public ChaosMonkeyJmxEndpoint chaosMonkeyJmxEndpoint() {
    return new ChaosMonkeyJmxEndpoint(settings());
  }
}
