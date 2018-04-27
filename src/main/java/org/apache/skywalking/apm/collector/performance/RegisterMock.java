/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.collector.performance;

import io.grpc.ManagedChannel;
import java.util.UUID;
import java.util.concurrent.*;
import org.apache.skywalking.apm.network.proto.*;
import org.joda.time.DateTime;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
class RegisterMock {

    private static final Logger logger = LoggerFactory.getLogger(RegisterMock.class);

    private ApplicationRegisterServiceGrpc.ApplicationRegisterServiceBlockingStub applicationRegisterServiceBlockingStub;
    private InstanceDiscoveryServiceGrpc.InstanceDiscoveryServiceBlockingStub instanceDiscoveryServiceBlockingStub;
    private ServiceNameDiscoveryServiceGrpc.ServiceNameDiscoveryServiceBlockingStub serviceNameDiscoveryServiceBlockingStub;

    void mock(ManagedChannel channel) throws InterruptedException {
        applicationRegisterServiceBlockingStub = ApplicationRegisterServiceGrpc.newBlockingStub(channel);
        instanceDiscoveryServiceBlockingStub = InstanceDiscoveryServiceGrpc.newBlockingStub(channel);
        serviceNameDiscoveryServiceBlockingStub = ServiceNameDiscoveryServiceGrpc.newBlockingStub(channel);
        registerConsumer();
        registerProvider();
    }

    private void registerConsumer() throws InterruptedException {
        Application.Builder application = Application.newBuilder();
        application.setApplicationCode("dubbox-consumer");

        ApplicationMapping applicationMapping;
        do {
            applicationMapping = applicationRegisterServiceBlockingStub.applicationCodeRegister(application.build());
            logger.debug("application id: {}", applicationMapping.getApplication().getValue());
            Thread.sleep(20);
        }
        while (applicationMapping.getApplication().getValue() == 0);

        ApplicationInstance.Builder instance = ApplicationInstance.newBuilder();
        instance.setApplicationId(applicationMapping.getApplication().getValue());
        instance.setAgentUUID(UUID.randomUUID().toString());
        instance.setRegisterTime(new DateTime("2017-01-01T00:01:01.001").getMillis());

        OSInfo.Builder osInfo = OSInfo.newBuilder();
        osInfo.setHostname("pengys");
        osInfo.setOsName("MacOS XX");
        osInfo.setProcessNo(1001);
        osInfo.addIpv4S("10.0.0.3");
        osInfo.addIpv4S("10.0.0.4");
        instance.setOsinfo(osInfo);

        ApplicationInstanceMapping instanceMapping;
        do {
            instanceMapping = instanceDiscoveryServiceBlockingStub.registerInstance(instance.build());
            logger.debug("instance id: {}", instanceMapping.getApplicationInstanceId());
            Thread.sleep(20);
        }
        while (instanceMapping.getApplicationInstanceId() == 0);

        ServiceNameCollection.Builder serviceNameCollection = ServiceNameCollection.newBuilder();
        ServiceNameElement.Builder serviceNameElement = ServiceNameElement.newBuilder();
        serviceNameElement.setApplicationId(applicationMapping.getApplication().getValue());
        serviceNameElement.setServiceName("org.skywaking.apm.testcase.dubbo.services.GreetService.doBusiness()");
        serviceNameElement.setSrcSpanType(SpanType.Exit);
        serviceNameCollection.addElements(serviceNameElement);

//        registerServiceName(serviceNameCollection);

        heartBeatScheduled(instanceMapping.getApplicationInstanceId());
    }

    private void registerProvider() throws InterruptedException {
        Application.Builder application = Application.newBuilder();
        application.setApplicationCode("dubbox-provider");

        ApplicationMapping applicationMapping;
        do {
            applicationMapping = applicationRegisterServiceBlockingStub.applicationCodeRegister(application.build());
            logger.debug("application id: {}", applicationMapping.getApplication().getValue());
            Thread.sleep(20);
        }
        while (applicationMapping.getApplication().getValue() == 0);

        ApplicationInstance.Builder instance = ApplicationInstance.newBuilder();
        instance.setApplicationId(applicationMapping.getApplication().getValue());
        instance.setAgentUUID(UUID.randomUUID().toString());
        instance.setRegisterTime(new DateTime("2017-01-01T00:01:01.001").getMillis());

        OSInfo.Builder osInfo = OSInfo.newBuilder();
        osInfo.setHostname("peng-yongsheng");
        osInfo.setOsName("MacOS X");
        osInfo.setProcessNo(1000);
        osInfo.addIpv4S("10.0.0.1");
        osInfo.addIpv4S("10.0.0.2");
        instance.setOsinfo(osInfo);

        ApplicationInstanceMapping instanceMapping;
        do {
            instanceMapping = instanceDiscoveryServiceBlockingStub.registerInstance(instance.build());
            logger.debug("instance id: {}", instanceMapping.getApplicationInstanceId());
            Thread.sleep(20);
        }
        while (instanceMapping.getApplicationInstanceId() == 0);

        ServiceNameCollection.Builder serviceNameCollection = ServiceNameCollection.newBuilder();
        ServiceNameElement.Builder serviceNameElement = ServiceNameElement.newBuilder();
        serviceNameElement.setApplicationId(applicationMapping.getApplication().getValue());
        serviceNameElement.setServiceName("org.skywaking.apm.testcase.dubbo.services.GreetService.doBusiness()");
        serviceNameElement.setSrcSpanType(SpanType.Entry);
        serviceNameCollection.addElements(serviceNameElement);

//        registerServiceName(serviceNameCollection);

        heartBeatScheduled(instanceMapping.getApplicationInstanceId());
    }

    private void registerServiceName(ServiceNameCollection.Builder serviceNameCollection) throws InterruptedException {
        ServiceNameMappingCollection serviceNameMappingCollection;
        do {
            logger.debug("register service name: {}", serviceNameCollection.getElements(0).getServiceName());
            serviceNameMappingCollection = serviceNameDiscoveryServiceBlockingStub.discovery(serviceNameCollection.build());
            logger.debug("service name mapping collection size: {}", serviceNameMappingCollection.getElementsCount());
            if (serviceNameMappingCollection.getElementsCount() > 0) {
                logger.debug("service id: {}", serviceNameMappingCollection.getElements(0).getServiceId());
            }
            Thread.sleep(20);
        }
        while (serviceNameMappingCollection.getElementsCount() == 0 || serviceNameMappingCollection.getElements(0).getServiceId() == 0);
    }

    private void heartBeatScheduled(int instanceId) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            new RunnableWithExceptionProtection(() -> heartBeat(instanceId),
                t -> logger.error("instance heart beat scheduled error.", t)), 4, 1, TimeUnit.SECONDS);
    }

    private void heartBeat(int instanceId) {
        long now = System.currentTimeMillis();
        logger.debug("instance heart beat, instance id: {}, time: {}", instanceId, now);
        ApplicationInstanceHeartbeat.Builder heartbeat = ApplicationInstanceHeartbeat.newBuilder();
        heartbeat.setApplicationInstanceId(instanceId);
        heartbeat.setHeartbeatTime(now);
        instanceDiscoveryServiceBlockingStub.heartbeat(heartbeat.build());
    }
}
