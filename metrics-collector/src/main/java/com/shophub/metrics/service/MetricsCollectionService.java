package com.shophub.metrics.service;

import com.shophub.metrics.config.ObservabilityConfig;
import com.shophub.metrics.model.ServiceMetrics;
import com.shophub.metrics.model.TraceData;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class MetricsCollectionService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsCollectionService.class);

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private ObservabilityConfig observabilityConfig;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private Tracer tracer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private Counter requestCounter;

    @Autowired
    private Timer requestTimer;

    @Autowired
    private Counter errorCounter;

    private final Map<String, ServiceMetrics> currentMetrics = new ConcurrentHashMap<>();
    private final Map<String, TraceData> activeTraces = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 30000)
    @Timed(value = "metrics.collection.time", description = "Time taken to collect metrics from all services")
    public void collectMetricsFromAllServices() {
        Span span = tracer.nextSpan().name("collect-all-metrics").start();
        
        try {
            logger.info("Starting metrics collection from all services");
            
            List<String> services = discoveryClient.getServices();
            logger.info("Discovered services: {}", services);
            
            for (String serviceName : services) {
                if (!"metrics-collector".equals(serviceName)) {
                    collectServiceMetrics(serviceName);
                }
            }
            
            updateObservabilityMetrics();
            cacheMetricsData();
            
            logger.info("Completed metrics collection for {} services", services.size());
            
        } catch (Exception e) {
            logger.error("Error during metrics collection", e);
            errorCounter.increment();
            span.tag("error", e.getMessage());
        } finally {
            span.end();
        }
    }

    public void collectServiceMetrics(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        
        for (ServiceInstance instance : instances) {
            try {
                ServiceMetrics metrics = new ServiceMetrics(serviceName, instance.getInstanceId());
                metrics.setStatus("UP");
                
                Map<String, Object> metricsData = new HashMap<>();
                metricsData.put("host", instance.getHost());
                metricsData.put("port", instance.getPort());
                metricsData.put("uri", instance.getUri().toString());
                metricsData.put("metadata", instance.getMetadata());
                metricsData.put("scheme", instance.getScheme());
                metricsData.put("lastUpdated", LocalDateTime.now());
                
                Random random = new Random();
                metrics.setResponseTime(50.0 + random.nextDouble() * 200);
                metrics.setRequestCount(random.nextInt(100) + 10);
                metrics.setErrorCount(random.nextInt(5));
                
                metrics.setMetrics(metricsData);
                currentMetrics.put(serviceName + "-" + instance.getInstanceId(), metrics);
                
                recordCustomMetrics(serviceName, metrics);
                
                logger.debug("Collected metrics for service: {} instance: {}", 
                    serviceName, instance.getInstanceId());
                
            } catch (Exception e) {
                logger.error("Failed to collect metrics for service: {} instance: {}", 
                    serviceName, instance.getInstanceId(), e);
                errorCounter.increment();
            }
        }
    }

    private void recordCustomMetrics(String serviceName, ServiceMetrics metrics) {
        meterRegistry.gauge("shophub.service.response.time", 
            Collections.singletonList(io.micrometer.core.instrument.Tag.of("service", serviceName)), 
            metrics.getResponseTime());
            
        meterRegistry.gauge("shophub.service.request.count", 
            Collections.singletonList(io.micrometer.core.instrument.Tag.of("service", serviceName)), 
            metrics.getRequestCount());
            
        meterRegistry.gauge("shophub.service.error.count", 
            Collections.singletonList(io.micrometer.core.instrument.Tag.of("service", serviceName)), 
            metrics.getErrorCount());
    }

    private void updateObservabilityMetrics() {
        observabilityConfig.getActiveTraces().set(activeTraces.size());
        observabilityConfig.getTotalRequests().addAndGet(currentMetrics.size());
    }

    private void cacheMetricsData() {
        try {
            String key = "shophub:metrics:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(key, currentMetrics, 5, TimeUnit.MINUTES);
            logger.debug("Cached metrics data with key: {}", key);
        } catch (Exception e) {
            logger.warn("Failed to cache metrics data", e);
        }
    }

    public TraceData startTrace(String serviceName, String operationName) {
        Span span = tracer.nextSpan().name(operationName).start();
        
        String traceId = span.context().traceId();
        String spanId = span.context().spanId();
        
        TraceData traceData = new TraceData(traceId, spanId, serviceName);
        traceData.setOperationName(operationName);
        
        Map<String, String> tags = new HashMap<>();
        tags.put("service.name", serviceName);
        tags.put("operation.name", operationName);
        traceData.setTags(tags);
        
        activeTraces.put(traceId, traceData);
        
        logger.debug("Started trace: {} for service: {} operation: {}", traceId, serviceName, operationName);
        
        return traceData;
    }

    public void finishTrace(String traceId) {
        TraceData traceData = activeTraces.remove(traceId);
        if (traceData != null) {
            traceData.finish();
            logger.debug("Finished trace: {} duration: {}ms", traceId, traceData.getDuration());
        }
    }

    public Map<String, ServiceMetrics> getCurrentMetrics() {
        return new HashMap<>(currentMetrics);
    }

    public Map<String, TraceData> getActiveTraces() {
        return new HashMap<>(activeTraces);
    }

    public List<String> getRegisteredServices() {
        return discoveryClient.getServices();
    }

    @Timed(value = "metrics.health.check", description = "Time taken to perform health check")
    public Map<String, Object> getHealthSummary() {
        Map<String, Object> health = new HashMap<>();
        health.put("totalServices", discoveryClient.getServices().size());
        health.put("activeMetrics", currentMetrics.size());
        health.put("activeTraces", activeTraces.size());
        health.put("lastCollection", LocalDateTime.now());
        
        Map<String, Long> serviceStatus = new HashMap<>();
        currentMetrics.values().forEach(metrics -> {
            String status = metrics.getStatus();
            serviceStatus.merge(status, 1L, Long::sum);
        });
        health.put("serviceStatus", serviceStatus);
        
        return health;
    }
}