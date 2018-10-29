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

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.skywalking.apm.collector.performance.mock.*;
import org.apache.skywalking.apm.collector.performance.register.RegisterInventoryStorage;
import org.apache.skywalking.apm.network.proto.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
class TraceSegmentMock {

    private static final Logger logger = LoggerFactory.getLogger(TraceSegmentMock.class);

    private final CompleteListener listener_1 = new CompleteListener();
    private final CompleteListener listener_2 = new CompleteListener();
    private final Config config;
    private final RegisterInventoryStorage.Service[] serviceAServices;
    private final RegisterInventoryStorage.Service[] serviceBServices;
    private final RegisterInventoryStorage.Service[] serviceCServices;

    private static final String COLLECTOR_1_ADDR = "localhost";
    //    private static final String COLLECTOR_1_ADDR = "192.168.3.111";
    private static final String COLLECTOR_2_ADDR = "localhost";
//    private static final String COLLECTOR_2_ADDR = "192.168.3.112";

    TraceSegmentMock(Config config,
        RegisterInventoryStorage.Service[] serviceAServices,
        RegisterInventoryStorage.Service[] serviceBServices,
        RegisterInventoryStorage.Service[] serviceCServices) {
        this.config = config;
        this.serviceAServices = serviceAServices;
        this.serviceBServices = serviceBServices;
        this.serviceCServices = serviceCServices;
    }

    void batchMock(int threadNum, AtomicLong segmentCounter, long startTime) {
        logger.info("thread {} start", threadNum);
        ManagedChannel channel_1 = ManagedChannelBuilder.forAddress(COLLECTOR_1_ADDR, 11800).usePlaintext(true).build();
        ManagedChannel channel_2 = ManagedChannelBuilder.forAddress(COLLECTOR_2_ADDR, 11800).usePlaintext(true).build();
        TraceSegmentServiceGrpc.TraceSegmentServiceStub stub_1 = TraceSegmentServiceGrpc.newStub(channel_1);
        TraceSegmentServiceGrpc.TraceSegmentServiceStub stub_2 = TraceSegmentServiceGrpc.newStub(channel_2);

        StreamObserver<UpstreamSegment> streamObserver_1 = createStreamObserver(threadNum, stub_1, listener_1);
        StreamObserver<UpstreamSegment> streamObserver_2 = createStreamObserver(threadNum, stub_2, listener_2);

        int cycle = 0;
        while (true) {
            long startTimestamp = System.currentTimeMillis() + cycle * 1000;
            for (int i = 0; i <= 1000; i++) {
                long counter = segmentCounter.incrementAndGet();
                if (counter % 10000 == 0) {
                    long duration = System.currentTimeMillis() - startTime;
                    long tps = counter / ((duration < 1000 ? 1000 : duration) / 1000);
                    logger.info("segment count: {}, tps: {}", counter, tps);
                }

                int instanceIndex = (int)counter % config.getServiceInstanceNumber();
                int endpointIndex = (int)counter % config.getEndpointNumber();

                UniqueId.Builder traceId = UniqueIdBuilder.INSTANCE.create();

                ServiceAMock serviceAMock = new ServiceAMock();
                UniqueId.Builder serviceASegmentId = UniqueIdBuilder.INSTANCE.create();
                serviceAMock.mock(streamObserver_1, traceId, serviceASegmentId, startTimestamp, serviceAServices[0], serviceBServices[0], instanceIndex, endpointIndex);

                ServiceBMock serviceBMock = new ServiceBMock();
                UniqueId.Builder serviceBSegmentId = UniqueIdBuilder.INSTANCE.create();
                serviceBMock.mock(streamObserver_2, traceId, serviceBSegmentId, serviceASegmentId, startTimestamp, serviceAServices[0], serviceBServices[0], serviceCServices[0], instanceIndex, endpointIndex);

                ServiceCMock serviceCMock = new ServiceCMock();
                UniqueId.Builder serviceCSegmentId = UniqueIdBuilder.INSTANCE.create();
                serviceCMock.mock(streamObserver_1, traceId, serviceCSegmentId, serviceBSegmentId, startTimestamp, serviceAServices[0], serviceBServices[0], serviceCServices[0], instanceIndex, endpointIndex);
            }
            streamObserver_1.onCompleted();
            streamObserver_2.onCompleted();

            while (!listener_1.isComplete() || !listener_2.isComplete()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            streamObserver_1 = createStreamObserver(threadNum, stub_1, listener_1);
            streamObserver_2 = createStreamObserver(threadNum, stub_2, listener_2);
            listener_1.reset();
            listener_2.reset();
            cycle++;
        }
    }

    private StreamObserver<UpstreamSegment> createStreamObserver(int threadNum,
        TraceSegmentServiceGrpc.TraceSegmentServiceStub stub, CompleteListener listener) {
        return stub.collect(new StreamObserver<Downstream>() {
            @Override public void onNext(Downstream downstream) {
            }

            @Override public void onError(Throwable throwable) {
                logger.error(throwable.getMessage(), throwable);
            }

            @Override public void onCompleted() {
                logger.info("thread {} on completed", threadNum);
                listener.onCompleted();
            }
        });
    }

    class CompleteListener {
        private volatile boolean isComplete = false;

        boolean isComplete() {
            return isComplete;
        }

        void onCompleted() {
            isComplete = true;
        }

        void reset() {
            isComplete = false;
        }
    }
}
