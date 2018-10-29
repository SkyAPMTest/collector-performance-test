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

package org.apache.skywalking.apm.collector.performance.mock;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.apache.skywalking.apm.collector.performance.register.RegisterInventoryStorage;
import org.apache.skywalking.apm.network.proto.*;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

/**
 * @author peng-yongsheng
 */
public class ServiceAMock {

    public void mock(StreamObserver<UpstreamSegment> segmentStreamObserver, UniqueId.Builder traceId,
        UniqueId.Builder segmentId, long startTimestamp, RegisterInventoryStorage.Service serviceAService,
        RegisterInventoryStorage.Service serviceBService, int instanceIndex, int endpointIndex) {
        UpstreamSegment.Builder upstreamSegment = UpstreamSegment.newBuilder();
        upstreamSegment.addGlobalTraceIds(traceId);
        upstreamSegment.setSegment(createSegment(startTimestamp, segmentId, serviceAService, serviceBService, instanceIndex, endpointIndex));

        segmentStreamObserver.onNext(upstreamSegment.build());
    }

    private ByteString createSegment(long startTimestamp, UniqueId.Builder segmentId,
        RegisterInventoryStorage.Service serviceAService,
        RegisterInventoryStorage.Service serviceBService, int instanceIndex, int endpointIndex) {
        TraceSegmentObject.Builder segment = TraceSegmentObject.newBuilder();
        segment.setTraceSegmentId(segmentId);
        segment.setApplicationId(serviceAService.getId());

        segment.setApplicationInstanceId(serviceAService.getServiceInstances()[instanceIndex].getId());
        segment.addSpans(createEntrySpan(startTimestamp, serviceAService, endpointIndex));

        for (int i = 1; i <= 20; i++) {
            segment.addSpans(createExitSpan(i, startTimestamp, serviceAService, serviceBService, instanceIndex, endpointIndex));
        }

        return segment.build().toByteString();
    }

    private SpanObject.Builder createEntrySpan(long startTimestamp, RegisterInventoryStorage.Service serviceAService,
        int endpointIndex) {
        SpanObject.Builder span = SpanObject.newBuilder();
        span.setSpanId(0);
        span.setSpanType(SpanType.Entry);
        span.setSpanLayer(SpanLayer.Http);
        span.setParentSpanId(-1);
        span.setStartTime(startTimestamp);
        span.setEndTime(startTimestamp + 1990);
        span.setComponentId(ComponentsDefine.TOMCAT.getId());
        span.setOperationNameId(serviceAService.getEntryEndpoints()[endpointIndex].getId());
        span.setIsError(false);
        return span;
    }

    private SpanObject.Builder createExitSpan(int spanId, long startTimestamp,
        RegisterInventoryStorage.Service serviceAService,
        RegisterInventoryStorage.Service serviceBService, int instanceIndex, int endpointIndex) {
        SpanObject.Builder span = SpanObject.newBuilder();
        span.setSpanId(spanId);
        span.setSpanType(SpanType.Exit);
        span.setSpanLayer(SpanLayer.RPCFramework);
        span.setParentSpanId(spanId - 1);
        span.setStartTime(startTimestamp + 10);
        span.setEndTime(startTimestamp + 1800);
        span.setComponentId(ComponentsDefine.DUBBO.getId());
        span.setOperationNameId(serviceAService.getExitEndpoints()[endpointIndex].getId());
        span.setPeerId(serviceBService.getServiceInstances()[instanceIndex].getNetworkAddressId());

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
}
