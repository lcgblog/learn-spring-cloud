package com.shophub.metrics.model;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ServiceMetrics {
    private String serviceName;
    private String instanceId;
    private LocalDateTime timestamp;
    private Map<String, Object> metrics;
    private String status;
    private Double responseTime;
    private Integer requestCount;
    private Integer errorCount;
    private Map<String, String> traceInfo;

    // Constructor
    public ServiceMetrics() {}

    public ServiceMetrics(String serviceName, String instanceId) {
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.timestamp = LocalDateTime.now();
        this.status = "UP";
    }

    // Getters and Setters
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Double responseTime) {
        this.responseTime = responseTime;
    }

    public Integer getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Integer requestCount) {
        this.requestCount = requestCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public Map<String, String> getTraceInfo() {
        return traceInfo;
    }

    public void setTraceInfo(Map<String, String> traceInfo) {
        this.traceInfo = traceInfo;
    }

    @Override
    public String toString() {
        return "ServiceMetrics{" +
                "serviceName='" + serviceName + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                ", responseTime=" + responseTime +
                ", requestCount=" + requestCount +
                ", errorCount=" + errorCount +
                '}';
    }
}