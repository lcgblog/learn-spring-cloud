package com.shophub.order.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义负载均衡器
 * 支持基于供应商的路由策略
 */
public class VendorAwareLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final Log log = LogFactory.getLog(VendorAwareLoadBalancer.class);


    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final AtomicInteger position = new AtomicInteger(0);
    
    public VendorAwareLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
                                  String serviceId) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }
    
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                .getIfAvailable(NoopServiceInstanceListSupplier::new);
        return serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new)
                .get(request)
                .next()
                .map(serviceInstances -> processInstanceResponse(supplier, serviceInstances));
    }
    
    private Response<ServiceInstance> processInstanceResponse(
            ServiceInstanceListSupplier supplier, List<ServiceInstance> serviceInstances) {

        if (serviceInstances.isEmpty()) {
            if (log.isWarnEnabled()) {
                log.warn("No servers available for service: " + serviceId);
            }
            return new EmptyResponse();
        }
        
        // 默认使用轮询策略
        int pos = Math.abs(this.position.incrementAndGet());
        ServiceInstance instance = serviceInstances.get(pos % serviceInstances.size());

        Response<ServiceInstance> serviceInstanceResponse = new DefaultResponse(instance);

        if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
            ((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
        }

        return serviceInstanceResponse;
    }
}