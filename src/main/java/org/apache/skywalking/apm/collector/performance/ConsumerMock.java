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
import java.util.Random;
import org.apache.skywalking.apm.collector.performance.register.ApplicationsStorage;
import org.apache.skywalking.apm.network.proto.*;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

/**
 * @author peng-yongsheng
 */
class ConsumerMock {

    private Random random = new Random();

    void mock(StreamObserver<UpstreamSegment> segmentStreamObserver, UniqueId.Builder globalTraceId,
        UniqueId.Builder segmentId, long startTimestamp, boolean isPrepare, ApplicationsStorage.Application application,
        int serviceIndex) {
        UpstreamSegment.Builder upstreamSegment = UpstreamSegment.newBuilder();
        upstreamSegment.addGlobalTraceIds(globalTraceId);
        upstreamSegment.setSegment(createSegment(startTimestamp, segmentId, isPrepare, application, serviceIndex));

        segmentStreamObserver.onNext(upstreamSegment.build());
    }

    private ByteString createSegment(long startTimestamp, UniqueId.Builder segmentId, boolean isPrepare,
        ApplicationsStorage.Application application,
        int serviceIndex) {
        TraceSegmentObject.Builder segment = TraceSegmentObject.newBuilder();
        segment.setTraceSegmentId(segmentId);
        segment.setApplicationId(application.getApplicationId());

        int index = random.nextInt(PerformanceTestBoot.INSTANCE_SIZE);
        segment.setApplicationInstanceId(application.getInstances()[index].getInstanceId());
        segment.addSpans(createExitSpan(startTimestamp, isPrepare, application, serviceIndex));
        segment.addSpans(createEntrySpan(startTimestamp, isPrepare, application, serviceIndex));

        return segment.build().toByteString();
    }

    private SpanObject.Builder createExitSpan(long startTimestamp, boolean isPrepare,
        ApplicationsStorage.Application application, int serviceIndex) {
        SpanObject.Builder span = SpanObject.newBuilder();
        span.setSpanId(1);
        span.setSpanType(SpanType.Exit);
        span.setSpanLayer(SpanLayer.RPCFramework);
        span.setParentSpanId(0);
        span.setStartTime(startTimestamp + 10);
        span.setEndTime(startTimestamp + 1990);
        span.setComponentId(ComponentsDefine.DUBBO.getId());
        if (isPrepare) {
            span.setPeer("172.25.0.4:20880");
            span.setOperationName("org.skywaking.apm.testcase.dubbo.services.GreetService.doBusiness()");
        } else {
            int serviceId = application.getExitServiceNames()[serviceIndex].getServiceId();
            span.setOperationNameId(serviceId);
            span.setPeerId(-1);
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
        span.setEndTime(startTimestamp + 2000);
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
