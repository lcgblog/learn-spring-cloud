package com.shophub.order.config;

import feign.RequestInterceptor;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign客户端分布式追踪配置
 * 解决Feign调用时traceId变化的问题
 */
@Configuration
public class FeignTracingConfiguration {

    @Autowired
    private Tracer tracer;

    /**
 * Feign请求拦截器，用于传播追踪上下文
 * 确保traceId在服务间调用时保持一致
 */
    @Bean
    public RequestInterceptor tracingRequestInterceptor() {
        return requestTemplate -> {
            if (tracer != null && tracer.currentSpan() != null) {
                // 获取当前span的追踪上下文
                var currentSpan = tracer.currentSpan();
                var traceContext = currentSpan.context();
                
                // 添加追踪头信息到Feign请求
                requestTemplate.header("X-Trace-Id", traceContext.traceId());
                requestTemplate.header("X-Span-Id", traceContext.spanId());
                
                // 添加B3追踪头（Zipkin标准）
                requestTemplate.header("X-B3-TraceId", traceContext.traceId());
                requestTemplate.header("X-B3-SpanId", traceContext.spanId());
                
                // 如果有父span，添加父span信息
                if (traceContext.parentId() != null) {
                    requestTemplate.header("X-B3-ParentSpanId", traceContext.parentId());
                }
                
                // 采样标志
                requestTemplate.header("X-B3-Sampled", "1");
                
                System.out.println("[Feign追踪] 传播traceId: " + traceContext.traceId() + ", spanId: " + traceContext.spanId());
            }
        };
    }
}