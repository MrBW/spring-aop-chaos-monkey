package de.codecentric.spring.boot.chaos.monkey.watcher.aspect;

import de.codecentric.spring.boot.chaos.monkey.component.ChaosMonkeyRequestScope;
import de.codecentric.spring.boot.chaos.monkey.component.ChaosTarget;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.actuate.health.Health;

@Aspect
@AllArgsConstructor
@Slf4j
public class SpringBootHealthIndicatorAspect extends ChaosMonkeyBaseAspect {

  private final ChaosMonkeyRequestScope chaosMonkeyRequestScope;

  @Pointcut("execution(* org.springframework.boot.actuate.health.HealthIndicator.getHealth(..))")
  public void getHealthPointCut() {}

  @Around("getHealthPointCut() && !classInChaosMonkeyPackage()")
  public Object intercept(ProceedingJoinPoint pjp) throws Throwable {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Health health;
    try {
      health = (Health) pjp.proceed();
      this.chaosMonkeyRequestScope.callChaosMonkey(
          ChaosTarget.ACTUATOR_HEALTH, createSignature(signature));
    } catch (final Exception e) {
      log.error("Exception occurred", e);
      health = Health.down(e).build();
    }
    return health;
  }
}
