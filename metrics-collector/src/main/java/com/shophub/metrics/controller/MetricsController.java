package com.shophub.metrics.controller;

import com.shophub.metrics.model.ServiceMetrics;
import com.shophub.metrics.model.TraceData;
import com.shophub.metrics.service.MetricsCollectionService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.NewSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);

    @Autowired
    private MetricsCollectionService metricsCollectionService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private Counter requestCounter;

    @Autowired
    private Tracer tracer;

    @GetMapping("/health")
    @Timed(value = "metrics.controller.health", description = "Health check endpoint response time")
    public ResponseEntity<Map<String, Object>> health() {
        requestCounter.increment();
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "metrics-collector");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");
        
        logger.info("Health check requested");
        return ResponseEntity.ok(health);
    }

    @GetMapping("/services")
    @NewSpan("get-registered-services")
    @Timed(value = "metrics.controller.services", description = "Get all registered services")
    public ResponseEntity<List<String>> getRegisteredServices() {
        requestCounter.increment();
        
        List<String> services = metricsCollectionService.getRegisteredServices();
        logger.info("Retrieved {} registered services", services.size());
        
        return ResponseEntity.ok(services);
    }

    @GetMapping("/current")
    @NewSpan("get-current-metrics")
    @Timed(value = "metrics.controller.current", description = "Get current metrics for all services")
    public ResponseEntity<Map<String, ServiceMetrics>> getCurrentMetrics() {
        requestCounter.increment();
        
        Map<String, ServiceMetrics> metrics = metricsCollectionService.getCurrentMetrics();
        logger.info("Retrieved current metrics for {} service instances", metrics.size());
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/traces")
    @NewSpan("get-active-traces")
    @Timed(value = "metrics.controller.traces", description = "Get active distributed traces")
    public ResponseEntity<Map<String, TraceData>> getActiveTraces() {
        requestCounter.increment();
        
        Map<String, TraceData> traces = metricsCollectionService.getActiveTraces();
        logger.info("Retrieved {} active traces", traces.size());
        
        return ResponseEntity.ok(traces);
    }

    @GetMapping("/summary")
    @NewSpan("get-health-summary")
    @Timed(value = "metrics.controller.summary", description = "Get observability health summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        requestCounter.increment();
        
        Map<String, Object> summary = metricsCollectionService.getHealthSummary();
        logger.info("Generated health summary");
        
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/trace/start")
    @NewSpan("start-custom-trace")
    @Timed(value = "metrics.controller.trace.start", description = "Start a custom trace")
    public ResponseEntity<TraceData> startTrace(
            @RequestParam String serviceName,
            @RequestParam String operationName) {
        requestCounter.increment();
        
        TraceData trace = metricsCollectionService.startTrace(serviceName, operationName);
        logger.info("Started custom trace: {} for service: {}", trace.getTraceId(), serviceName);
        
        return ResponseEntity.ok(trace);
    }

    @PostMapping("/trace/finish/{traceId}")
    @NewSpan("finish-custom-trace")
    @Timed(value = "metrics.controller.trace.finish", description = "Finish a custom trace")
    public ResponseEntity<Map<String, String>> finishTrace(@PathVariable String traceId) {
        requestCounter.increment();
        
        metricsCollectionService.finishTrace(traceId);
        
        Map<String, String> response = new HashMap<>();
        response.put("traceId", traceId);
        response.put("status", "finished");
        response.put("timestamp", LocalDateTime.now().toString());
        
        logger.info("Finished custom trace: {}", traceId);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/collect")
    @NewSpan("manual-metrics-collection")
    @Timed(value = "metrics.controller.collect", description = "Manually trigger metrics collection")
    public ResponseEntity<Map<String, Object>> triggerCollection() {
        requestCounter.increment();
        
        try {
            metricsCollectionService.collectMetricsFromAllServices();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Metrics collection triggered successfully");
            response.put("timestamp", LocalDateTime.now());
            
            logger.info("Manual metrics collection triggered");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to trigger metrics collection", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to trigger metrics collection: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/observability/features")
    @NewSpan("get-observability-features")
    @Timed(value = "metrics.controller.features", description = "Get observability features status")
    public ResponseEntity<Map<String, Object>> getObservabilityFeatures() {
        requestCounter.increment();
        
        Map<String, Object> features = new HashMap<>();
        features.put("distributedTracing", true);
        features.put("metricsCollection", true);
        features.put("customMetrics", true);
        features.put("prometheusExport", true);
        features.put("zipkinIntegration", true);
        features.put("redisCache", true);
        features.put("serviceDiscovery", true);
        features.put("healthChecks", true);
        
        Map<String, Object> config = new HashMap<>();
        config.put("collectionInterval", "30s");
        config.put("tracingSampleRate", "1.0");
        config.put("cacheExpiration", "5m");
        
        features.put("configuration", config);
        features.put("timestamp", LocalDateTime.now());
        
        logger.info("Observability features requested");
        
        return ResponseEntity.ok(features);
    }

    @GetMapping("/testSpan")
    @Observed(name = "test-span-main")
    public String testSpan()throws Exception{
        span1();
        span2();
        metricsCollectionService.span3();
        metricsCollectionService.span4();
        return "completed";
    }

    //不会生效
    @Observed
    private void span1()throws Exception{
        logger.info("Span1 start");
        TimeUnit.SECONDS.sleep(1);
    }

    //不会生效
    @Observed
    public String span2()throws Exception{
        logger.info("Span2 start");
        TimeUnit.SECONDS.sleep(1);
        logger.info("Span2 done");
        return "span2";
    }
}