package com.shophub.metrics.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ObservabilityConfig {

    private final AtomicInteger activeTraces = new AtomicInteger(0);
    private final AtomicInteger totalRequests = new AtomicInteger(0);

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "shophub-metrics-collector");
    }

    @Bean
    public Counter requestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("shophub.requests.total")
                .description("Total number of requests across all services")
                .register(meterRegistry);
    }

    @Bean
    public Timer requestTimer(MeterRegistry meterRegistry) {
        return Timer.builder("shophub.requests.duration")
                .description("Request processing time")
                .register(meterRegistry);
    }

    @Bean
    public Gauge activeTracesGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("shophub.traces.active", activeTraces, AtomicInteger::doubleValue)
                .description("Number of active distributed traces")
                .register(meterRegistry);
    }

    @Bean
    public Counter errorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("shophub.errors.total")
                .description("Total number of errors across all services")
                .register(meterRegistry);
    }

    @Bean
    public Counter circuitBreakerCounter(MeterRegistry meterRegistry) {
        return Counter.builder("shophub.circuitbreaker.calls")
                .description("Circuit breaker state changes")
                .register(meterRegistry);
    }

    public AtomicInteger getActiveTraces() {
        return activeTraces;
    }

    public AtomicInteger getTotalRequests() {
        return totalRequests;
    }
}