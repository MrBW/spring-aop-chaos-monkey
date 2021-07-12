package de.codecentric.spring.boot.chaos.monkey.assaults;

import com.sun.management.OperatingSystemMXBean;
import de.codecentric.spring.boot.chaos.monkey.component.MetricEventPublisher;
import de.codecentric.spring.boot.chaos.monkey.component.MetricType;
import de.codecentric.spring.boot.chaos.monkey.configuration.ChaosMonkeySettings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpuAssault implements ChaosMonkeyRuntimeAssault {
  private static final Logger Logger = LoggerFactory.getLogger(CpuAssault.class);

  private final ChaosMonkeySettings settings;

  private final MetricEventPublisher metricEventPublisher;
  private final OperatingSystemMXBean os;

  public CpuAssault(
      OperatingSystemMXBean os,
      ChaosMonkeySettings settings,
      MetricEventPublisher metricEventPublisher) {
    this.os = os;
    this.settings = settings;
    this.metricEventPublisher = metricEventPublisher;
  }

  @Override
  public boolean isActive() {
    return settings.getAssaultProperties().isCpuActive();
  }

  @Override
  public void attack() {
    Logger.info("Chaos Monkey - cpu assault");

    // metrics
    if (metricEventPublisher != null) {
      metricEventPublisher.publishMetricEvent(MetricType.CPU_ASSAULT);
    }
    double load = settings.getAssaultProperties().getCpuLoadTargetFraction();

    List<Thread> threads = new ArrayList<>();
    for (int num = 0; os.getProcessCpuLoad() < load; num++) {
      Thread thread =
          new Thread(
              () -> {
                long f1 = 0;
                long f2 = 1;
                while (!Thread.interrupted()) {
                  // next fibonacci number
                  f2 = f1 + f2;
                  f1 = f2 - f1;
                }
              },
              "CPU Assault thread " + num);
      threads.add(thread);
      thread.start();
    }
    waitUntil(settings.getAssaultProperties().getCpuMillisecondsHoldLoad());
    for (Thread thread : threads) {
      thread.interrupt();
      while (thread.isAlive()) {
        try {
          thread.join();
        } catch (InterruptedException e) {
          thread.interrupt();
        }
      }
    }
    Logger.info("Chaos Monkey - cpu assault cleaned up");
  }

  private void waitUntil(int ms) {
    final long startNano = System.nanoTime();
    long now = startNano;
    while (startNano + TimeUnit.MILLISECONDS.toNanos(ms) > now && isActive()) {
      try {
        long elapsed = TimeUnit.NANOSECONDS.toMillis(startNano - now);
        Thread.sleep(Math.min(100, ms - elapsed));
        now = System.nanoTime();
      } catch (InterruptedException e) {
        break;
      }
    }
  }
}
