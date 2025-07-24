package com.shophub.order.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 负载均衡配置
 * 自定义负载均衡策略，演示客户端负载均衡
 */
@Configuration
public class LoadBalancerConfig {
    
    /**
     * 为产品服务配置随机负载均衡策略
     * 也可以使用轮询 (RoundRobinLoadBalancer) 或自定义策略
     */
//    @Bean
//    public ReactorLoadBalancer<ServiceInstance> productServiceLoadBalancer(
//            Environment environment,
//            LoadBalancerClientFactory loadBalancerClientFactory) {
//
//        final String name = "product-service";
//
//        return new RandomLoadBalancer(
//            loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
//            name
//        );
//    }


    @Bean
    public ReactorLoadBalancer<ServiceInstance> productServiceLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {

        String name = "product-service";
        return new VendorAwareLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class)
                , name);
    }
}