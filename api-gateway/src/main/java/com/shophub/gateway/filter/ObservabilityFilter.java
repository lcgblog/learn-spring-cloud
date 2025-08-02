package com.shophub.gateway.filter;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import reactor.core.publisher.Mono;

@Component
public class ObservabilityFilter implements GatewayFilter {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityFilter.class);

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private Tracer tracer;

    private final Counter requestCounter;
    private final Timer requestTimer;
    private final Counter errorCounter;

    public ObservabilityFilter(MeterRegistry meterRegistry) {
        this.requestCounter = Counter.builder("gateway.requests.total")
                .description("Total number of requests through gateway")
                .register(meterRegistry);
                
        this.requestTimer = Timer.builder("gateway.requests.duration")
                .description("Request processing time through gateway")
                .register(meterRegistry);
                
        this.errorCounter = Counter.builder("gateway.errors.total")
                .description("Total number of errors through gateway")
                .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant startTime = Instant.now();
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        // Start a new span for this request
        Span span = tracer.nextSpan()
                .name("gateway-request")
                .tag("http.method", method)
                .tag("http.path", path)
                .tag("component", "api-gateway")
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            
            // Add tracing headers to the request
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-Trace-Id", span.context().traceId())
                            .header("X-Span-Id", span.context().spanId())
                            .build())
                    .build();

            logger.info("Gateway processing request: {} {} with trace: {}", 
                    method, path, span.context().traceId());

            requestCounter.increment();

            return chain.filter(modifiedExchange)
                    .doOnSuccess(aVoid -> {
                        Duration duration = Duration.between(startTime, Instant.now());
                        requestTimer.record(duration);
                        
                        HttpStatusCode status = modifiedExchange.getResponse().getStatusCode();
                        int statusCode = status != null ? status.value() : 0;
                        span.tag("http.status_code", String.valueOf(statusCode));
                        
                        if (statusCode >= 400) {
                            errorCounter.increment();
                            span.tag("error", "true");
                        }
                        
                        logger.info("Gateway completed request: {} {} in {}ms with status: {}", 
                                method, path, duration.toMillis(), statusCode);
                    })
                    .doOnError(throwable -> {
                        Duration duration = Duration.between(startTime, Instant.now());
                        requestTimer.record(duration);
                        errorCounter.increment();
                        
                        span.tag("error", "true");
                        span.tag("error.message", throwable.getMessage());
                        
                        logger.error("Gateway error processing request: {} {} in {}ms", 
                                method, path, duration.toMillis(), throwable);
                    })
                    .doFinally(signalType -> {
                        span.end();
                    });
                    
        } catch (Exception e) {
            span.tag("error", "true");
            span.tag("error.message", e.getMessage());
            errorCounter.increment();
            throw e;
        }
    }
}