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
import org.apache.skywalking.apm.collector.performance.register.ApplicationsStorage;
import org.apache.skywalking.apm.network.proto.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
class TraceSegmentMock {

    private static final Logger logger = LoggerFactory.getLogger(TraceSegmentMock.class);

    private final CompleteListener listener = new CompleteListener();
    private final ApplicationsStorage.Application[] providerApplications;
    private final ApplicationsStorage.Application[] consumerApplications;

    TraceSegmentMock(
        ApplicationsStorage.Application[] providerApplications,
        ApplicationsStorage.Application[] consumerApplications) {
        this.providerApplications = providerApplications;
        this.consumerApplications = consumerApplications;
    }

    void batchMock(int threadNum, AtomicLong segmentCounter, long startTime) {
        logger.info("thread {} start", threadNum);
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 11800).usePlaintext(true).build();
        TraceSegmentServiceGrpc.TraceSegmentServiceStub stub = TraceSegmentServiceGrpc.newStub(channel);

        StreamObserver<UpstreamSegment> streamObserver = createStreamObserver(threadNum, stub);

        int cycle = 0;
        while (true) {
            long startTimestamp = System.currentTimeMillis() + cycle * 1000;
            for (int i = 0; i <= 1000; i++) {
                long counter = segmentCounter.incrementAndGet();
                if (counter % 40000 == 0) {
                    long duration = System.currentTimeMillis() - startTime;
                    long tps = counter / ((duration < 1000 ? 1000 : duration) / 1000);
                    logger.info("segment count: {}, tps: {}", counter, tps);
                }

                int appIndex = (int)counter % PerformanceTestBoot.APPLICATION_SIZE;
                int serviceIndex = (int)counter % PerformanceTestBoot.SERVICE_SIZE;

                UniqueId.Builder globalTraceId = UniqueIdBuilder.INSTANCE.create();

                ConsumerMock consumerMock = new ConsumerMock();
                UniqueId.Builder consumerSegmentId = UniqueIdBuilder.INSTANCE.create();
                consumerMock.mock(streamObserver, globalTraceId, consumerSegmentId, startTimestamp, false, consumerApplications[appIndex], serviceIndex);

                ProviderMock providerMock = new ProviderMock();
                UniqueId.Builder providerSegmentId = UniqueIdBuilder.INSTANCE.create();
                providerMock.mock(streamObserver, globalTraceId, providerSegmentId, consumerSegmentId, startTimestamp, false, providerApplications[appIndex], serviceIndex, consumerApplications[appIndex]);
            }
            streamObserver.onCompleted();

            while (!listener.isComplete()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            streamObserver = createStreamObserver(threadNum, stub);
            listener.reset();
            cycle++;
        }
    }

    private StreamObserver<UpstreamSegment> createStreamObserver(int threadNum,
        TraceSegmentServiceGrpc.TraceSegmentServiceStub stub) {
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
