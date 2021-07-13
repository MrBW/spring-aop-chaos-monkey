/*
 * Copyright 2021 the original author or authors.
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
package de.codecentric.spring.boot.chaos.monkey.watcher.aspect;

import static org.junit.jupiter.api.Assertions.assertThrows;

import de.codecentric.spring.boot.demo.chaos.monkey.ChaosDemoApplication;
import de.codecentric.spring.boot.demo.chaos.monkey.repository.CrudDemoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
      "chaos.monkey.watcher.repository=true",
      "chaos.monkey.assaults.level=1",
      "chaos.monkey.assaults.exceptions-active=true",
      "chaos.monkey.enabled=true"
    },
    classes = {ChaosDemoApplication.class})
@ActiveProfiles("chaos-monkey")
public class SpringRepositoryAspectRepositoryInterfaceTest {

  @Autowired private CrudDemoRepository target;

  @Test
  public void testRepoIsAttackedWhenRepoWatcherIsActive() {
    assertThrows(Exception.class, () -> target.findAll());
  }
}
