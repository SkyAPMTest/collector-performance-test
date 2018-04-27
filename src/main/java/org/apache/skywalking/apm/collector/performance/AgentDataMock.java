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
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class AgentDataMock {

    private static final Logger logger = LoggerFactory.getLogger(AgentDataMock.class);

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 11800).usePlaintext(true).build();

        RegisterMock registerMock = new RegisterMock();
        registerMock.mock(channel);

        TraceSegmentMock segmentMock = new TraceSegmentMock();
        segmentMock.singleMock(channel, System.currentTimeMillis());

        logger.info("waiting...");
        Thread.sleep(30000);

        AtomicLong segmentCounter = new AtomicLong(0);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            new Thread(new TraceSegmentMockRunnable(i, segmentCounter, startTime)).start();
        }

//        JVMMetricMock jvmMetricMock = new JVMMetricMock();
//        jvmMetricMock.mock(channel, times);
    }

    static class TraceSegmentMockRunnable implements Runnable {

        private final AtomicLong segmentCounter;
        private final long startTime;
        private final int num;

        TraceSegmentMockRunnable(int num, AtomicLong segmentCounter, long startTime) {
            this.segmentCounter = segmentCounter;
            this.startTime = startTime;
            this.num = num;
        }

        @Override public void run() {
            TraceSegmentMock newSegmentMock = new TraceSegmentMock();
            newSegmentMock.batchMock(num, segmentCounter, startTime);
        }
    }
}
