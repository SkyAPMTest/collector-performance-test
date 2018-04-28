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

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.apache.skywalking.apm.collector.performance.register.*;
import org.apache.skywalking.apm.network.proto.*;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

/**
 * @author peng-yongsheng
 */
class ConsumerMock {

    void mock(StreamObserver<UpstreamSegment> segmentStreamObserver, UniqueId.Builder globalTraceId,
        UniqueId.Builder segmentId, long startTimestamp, boolean isPrepare, ApplicationsStorage.Application application,
        int serviceIndex, int instanceIndex) {
        UpstreamSegment.Builder upstreamSegment = UpstreamSegment.newBuilder();
        upstreamSegment.addGlobalTraceIds(globalTraceId);
        upstreamSegment.setSegment(createSegment(startTimestamp, segmentId, isPrepare, application, serviceIndex, instanceIndex));

        segmentStreamObserver.onNext(upstreamSegment.build());
    }

    private ByteString createSegment(long startTimestamp, UniqueId.Builder segmentId, boolean isPrepare,
        ApplicationsStorage.Application application,
        int serviceIndex, int instanceIndex) {
        TraceSegmentObject.Builder segment = TraceSegmentObject.newBuilder();
        segment.setTraceSegmentId(segmentId);
        segment.setApplicationId(application.getApplicationId());

        segment.setApplicationInstanceId(application.getInstances()[instanceIndex].getInstanceId());
        segment.addSpans(createEntrySpan(startTimestamp, isPrepare, application, serviceIndex));

        for (int i = 1; i <= 20; i++) {
            segment.addSpans(createExitSpan(i, startTimestamp, isPrepare, application, serviceIndex));
        }

        return segment.build().toByteString();
    }

    private SpanObject.Builder createExitSpan(int spanId, long startTimestamp, boolean isPrepare,
        ApplicationsStorage.Application application, int serviceIndex) {
        SpanObject.Builder span = SpanObject.newBuilder();
        span.setSpanId(spanId);
        span.setSpanType(SpanType.Exit);
        span.setSpanLayer(SpanLayer.RPCFramework);
        span.setParentSpanId(spanId - 1);
        span.setStartTime(startTimestamp + 10);
        span.setEndTime(startTimestamp + 1700);
        span.setComponentId(ComponentsDefine.DUBBO.getId());
        if (isPrepare) {
            span.setPeer("172.25.0.4:20880");
            span.setOperationName("org.skywaking.apm.testcase.dubbo.services.GreetService.doBusiness()");
        } else {
            int serviceId = application.getExitServiceNames()[serviceIndex].getServiceId();
            span.setOperationNameId(serviceId);
            span.setPeerId(NetworkRegister.CONSUMER_NETWORK_ID);
        }

        if (spanId > 14) {
            KeyWithStringValue.Builder value1 = KeyWithStringValue.newBuilder();
            value1.setKey("error message");
            value1.setValue("[INFO] Building jar: /Users/pengys5/code/sky-walking/collector-performance-test/target/collector-performance-test-1.0-jar-with-dependencies.jar");

            KeyWithStringValue.Builder value2 = KeyWithStringValue.newBuilder();
            value2.setKey("error message");
            value2.setValue("[INFO] Building jar: /Users/pengys5/code/sky-walking/collector-performance-test/target/collector-performance-test-1.0-jar-with-dependencies.jar");

            LogMessage.Builder logMessage = LogMessage.newBuilder();
            logMessage.addData(value1);
            logMessage.addData(value2);
            span.addLogs(logMessage.build());
        }
        span.setIsError(false);
        return span;
    }

    private SpanObject.Builder createEntrySpan(long startTimestamp, boolean isPrepare,
        ApplicationsStorage.Application application, int serviceIndex) {
        SpanObject.Builder span = SpanObject.newBuilder();
        span.setSpanId(0);
        span.setSpanType(SpanType.Entry);
        span.setSpanLayer(SpanLayer.Http);
        span.setParentSpanId(-1);
        span.setStartTime(startTimestamp);
        span.setEndTime(startTimestamp + 1990);
        span.setComponentId(ComponentsDefine.TOMCAT.getId());
        if (isPrepare) {
            span.setOperationName("/dubbox-case/case/dubbox-rest");
        } else {
            int serviceId = application.getEntryServiceNames()[serviceIndex].getServiceId();
            span.setOperationNameId(serviceId);
        }
        span.setIsError(false);
        return span;
    }
}
