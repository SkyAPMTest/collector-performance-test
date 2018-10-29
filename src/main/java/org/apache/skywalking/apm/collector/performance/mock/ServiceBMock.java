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
public class ServiceBMock {

    public void mock(StreamObserver<UpstreamSegment> segmentStreamObserver, UniqueId.Builder traceId,
        UniqueId.Builder segmentId, UniqueId.Builder parentTraceSegmentId, long startTimestamp,
        RegisterInventoryStorage.Service serviceAService, RegisterInventoryStorage.Service serviceBService,
        RegisterInventoryStorage.Service serviceCService, int instanceIndex, int endpointIndex) {
        UpstreamSegment.Builder upstreamSegment = UpstreamSegment.newBuilder();
        upstreamSegment.addGlobalTraceIds(traceId);
        upstreamSegment.setSegment(createSegment(startTimestamp, segmentId, parentTraceSegmentId, serviceAService, serviceBService, serviceCService, instanceIndex, endpointIndex));

        segmentStreamObserver.onNext(upstreamSegment.build());
    }

    private ByteString createSegment(long startTimestamp, UniqueId.Builder segmentId,
        UniqueId.Builder parentTraceSegmentId, RegisterInventoryStorage.Service serviceAService,
        RegisterInventoryStorage.Service serviceBService, RegisterInventoryStorage.Service serviceCService,
        int instanceIndex, int endpointIndex) {
        TraceSegmentObject.Builder segment = TraceSegmentObject.newBuilder();
        segment.setTraceSegmentId(segmentId);
        segment.setApplicationId(serviceBService.getId());

        segment.setApplicationInstanceId(serviceBService.getServiceInstances()[instanceIndex].getId());
        segment.addSpans(createEntrySpan(startTimestamp, parentTraceSegmentId, serviceAService, serviceBService, instanceIndex, endpointIndex));

        for (int i = 1; i <= 20; i++) {
            segment.addSpans(createExitSpan(i, startTimestamp, serviceBService, serviceCService, instanceIndex, endpointIndex));
        }

        return segment.build().toByteString();
    }

    private SpanObject.Builder createEntrySpan(long startTimestamp, UniqueId.Builder uniqueId,
        RegisterInventoryStorage.Service serviceAService, RegisterInventoryStorage.Service serviceBService,
        int instanceIndex, int endpointIndex) {
        SpanObject.Builder span = SpanObject.newBuilder();
        span.setSpanId(0);
        span.setSpanType(SpanType.Entry);
        span.setSpanLayer(SpanLayer.RPCFramework);
        span.setParentSpanId(-1);
        span.setStartTime(startTimestamp + 200);
        span.setEndTime(startTimestamp + 1700);
        span.setComponentId(ComponentsDefine.DUBBO.getId());
        span.setIsError(false);
        span.addRefs(createReference(uniqueId, serviceAService, serviceBService, instanceIndex, endpointIndex));
        span.setOperationNameId(serviceBService.getEntryEndpoints()[endpointIndex].getId());
        return span;
    }

    private TraceSegmentReference.Builder createReference(UniqueId.Builder parentTraceSegmentId,
        RegisterInventoryStorage.Service serviceAService, RegisterInventoryStorage.Service serviceBService,
        int instanceIndex, int endpointIndex) {
        TraceSegmentReference.Builder reference = TraceSegmentReference.newBuilder();
        reference.setParentTraceSegmentId(parentTraceSegmentId);
        reference.setParentApplicationInstanceId(serviceAService.getServiceInstances()[instanceIndex].getId());
        reference.setParentSpanId(1);
        reference.setEntryApplicationInstanceId(serviceAService.getServiceInstances()[instanceIndex].getId());
        reference.setRefType(RefType.CrossProcess);

        int endpointId = serviceAService.getEntryEndpoints()[endpointIndex].getId();
        reference.setParentServiceId(endpointId);
        reference.setNetworkAddressId(serviceBService.getServiceInstances()[instanceIndex].getNetworkAddressId());
        reference.setEntryServiceId(endpointId);
        return reference;
    }

    private SpanObject.Builder createExitSpan(int spanId, long startTimestamp,
        RegisterInventoryStorage.Service serviceBService, RegisterInventoryStorage.Service serviceCService,
        int instanceIndex, int endpointIndex) {
        SpanObject.Builder span = SpanObject.newBuilder();
        span.setSpanId(spanId);
        span.setSpanType(SpanType.Exit);
        span.setSpanLayer(SpanLayer.MQ);
        span.setParentSpanId(spanId - 1);
        span.setStartTime(startTimestamp + 310);
        span.setEndTime(startTimestamp + 1600);
        span.setComponentId(ComponentsDefine.ROCKET_MQ.getId());
        span.setIsError(false);
        span.setOperationNameId(serviceBService.getExitEndpoints()[endpointIndex].getId());
        span.setPeerId(serviceCService.getServiceInstances()[instanceIndex].getNetworkAddressId());

        if (spanId > 14) {
            KeyWithStringValue.Builder value1 = KeyWithStringValue.newBuilder();
            value1.setKey("message");
            value1.setValue("org.apache.skywalking.apm.collector.performance.PerformanceBoot - 52 [main] INFO  [] - service A's endpoint register finish," +
                "org.apache.skywalking.apm.collector.performance.PerformanceBoot - 56 [main] INFO  [] - service B's endpoint register finish" +
                "org.apache.skywalking.apm.collector.performance.PerformanceBoot - 60 [main] INFO  [] - service C's endpoint register finish");
            LogMessage.Builder logMessage = LogMessage.newBuilder();
            logMessage.addData(value1);
            span.addLogs(logMessage.build());
        }
        return span;
    }
}
