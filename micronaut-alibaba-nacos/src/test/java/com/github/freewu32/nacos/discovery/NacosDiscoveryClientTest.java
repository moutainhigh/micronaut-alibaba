package com.github.freewu32.nacos.discovery;

import com.github.freewu32.nacos.client.NacosClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.runtime.Micronaut;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class NacosDiscoveryClientTest {

    @Test
    void getInstances() {
        ApplicationContext context = Micronaut.build(new String[]{})
                .environments(Environment.TEST).start();

        try {
            String serviceName = context.getEnvironment().get("micronaut.application.name",
                    String.class).get();

            NacosClient discoveryClient = context.getBean(NacosClient.class);

            ServiceInstance instance = Flowable.fromPublisher(discoveryClient
                    .getInstances(serviceName)).blockingSingle().get(0);

            Assertions.assertEquals(instance.getId(), "DEFAULT_GROUP@@" + serviceName);
        } finally {
            context.stop();
        }
    }

    @Test
    void getServiceIds() {
        ApplicationContext context = Micronaut.build(new String[]{})
                .environments(Environment.TEST).start();

        try {
            String serviceName = context.getEnvironment().get("micronaut.application.name",
                    String.class).get();

            NacosClient discoveryClient = context.getBean(NacosClient.class);

            List<String> serviceIds = Flowable.fromPublisher(discoveryClient
                    .getServiceIds()).blockingSingle();

            Assertions.assertLinesMatch(serviceIds, Collections.singletonList(serviceName));
        } finally {
            context.stop();
        }
    }
}