package de.codecentric.spring.boot.chaos.monkey.component;

import de.codecentric.spring.boot.chaos.monkey.events.MetricEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Benjamin Wilms
 */
public class MetricEventPublisher implements ApplicationEventPublisherAware {
    private ApplicationEventPublisher publisher;

    public void publishMetricEvent(String signature, MetricType metricType, String... tags) {
        MetricEvent metricEvent = new MetricEvent(this, metricType, signature, tags);

        publisher.publishEvent(metricEvent);
    }

    public void publishMetricEvent(MetricType metricType, String... tags) {
        MetricEvent metricEvent = new MetricEvent(this, metricType, tags);

        publisher.publishEvent(metricEvent);
    }

    public void publishMetricEvent(MetricType metricType, AtomicInteger atomicTimeoutGauge) {
        this.publishMetricEvent(metricType, atomicTimeoutGauge == null ? -1 : atomicTimeoutGauge.longValue());
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    public void publishMetricEvent(MetricType type, long metricValue) {
        publisher.publishEvent(new MetricEvent(this, type, metricValue, null));
    }
}
