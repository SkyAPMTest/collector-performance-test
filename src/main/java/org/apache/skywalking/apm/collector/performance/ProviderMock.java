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
class ProviderMock {

    void mock(StreamObserver<UpstreamSegment> segmentStreamObserver, UniqueId.Builder globalTraceId,
        UniqueId.Builder segmentId, UniqueId.Builder parentTraceSegmentId, long startTimestamp, boolean isPrepare,
        ApplicationsStorage.Application providerApplication, int serviceIndex,
        ApplicationsStorage.Application consumerApplication, int instanceIndex) {
        UpstreamSegment.Builder upstreamSegment = UpstreamSegment.newBuilder();
        upstreamSegment.addGlobalTraceIds(globalTraceId);
        upstreamSegment.setSegment(createSegment(startTimestamp, segmentId, parentTraceSegmentId, isPrepare, providerApplication, serviceIndex, consumerApplication, instanceIndex));

        segmentStreamObserver.onNext(upstreamSegment.build());
    }

    private ByteString createSegment(long startTimestamp, UniqueId.Builder segmentId,
        UniqueId.Builder parentTraceSegmentId, boolean isPrepare, ApplicationsStorage.Application providerApplication,
        int serviceIndex,
        ApplicationsStorage.Application consumerApplication, int instanceIndex) {
        TraceSegmentObject.Builder segment = TraceSegmentObject.newBuilder();
        segment.setTraceSegmentId(segmentId);
        segment.setApplicationId(providerApplication.getApplicationId());

        segment.setApplicationInstanceId(providerApplication.getInstances()[instanceIndex].getInstanceId());
        segment.addSpans(createEntrySpan(startTimestamp, parentTraceSegmentId, isPrepare, providerApplication, serviceIndex, consumerApplication, instanceIndex));

        for (int i = 1; i <= 20; i++) {
            segment.addSpans(createExitSpan(i, startTimestamp, isPrepare));
        }

        return segment.build().toByteString();
    }

    private TraceSegmentReference.Builder createReference(UniqueId.Builder parentTraceSegmentId, boolean isPrepare,
        ApplicationsStorage.Application consumerApplication, int serviceIndex, int instanceIndex) {
        TraceSegmentReference.Builder reference = TraceSegmentReference.newBuilder();
        reference.setParentTraceSegmentId(parentTraceSegmentId);
        reference.setParentApplicationInstanceId(consumerApplication.getInstances()[instanceIndex].getInstanceId());
        reference.setParentSpanId(1);
        reference.setEntryApplicationInstanceId(consumerApplication.getInstances()[instanceIndex].getInstanceId());
        reference.setRefType(RefType.CrossProcess);

        int serviceId = consumerApplication.getEntryServiceNames()[serviceIndex].getServiceId();

        if (isPrepare) {
            reference.setParentServiceName("/dubbox-case/case/dubbox-rest");
            reference.setNetworkAddress("172.25.0.4:20880");
            reference.setEntryServiceName("/dubbox-case/case/dubbox-rest");
        } else {
            reference.setParentServiceId(serviceId);
            reference.setNetworkAddressId(NetworkRegister.CONSUMER_NETWORK_ID);
            reference.setEntryServiceId(serviceId);
        }
        return reference;
    }

    private SpanObject.Builder createExitSpan(int spanId, long startTimestamp, boolean isPrepare) {
        SpanObject.Builder span = SpanObject.newBuilder();
        span.setSpanId(spanId);
        span.setSpanType(SpanType.Exit);
        span.setSpanLayer(SpanLayer.Database);
        span.setParentSpanId(spanId - 1);
        span.setStartTime(startTimestamp + 510);
        span.setEndTime(startTimestamp + 1490);
        span.setComponentId(ComponentsDefine.MONGODB.getId());
        span.setIsError(false);

        if (isPrepare) {
            span.setOperationName("mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]");
            span.setPeer("localhost:27017");
        } else {
            span.setOperationNameId(-1);
            span.setPeerId(NetworkRegister.PROVIDER_NETWORK_ID);
        }

        if (spanId > 14) {
            KeyWithStringValue.Builder value1 = KeyWithStringValue.newBuilder();
            value1.setKey("sql");
            value1.setValue("select columnA, columnB, columnC, columnD, columnE, columnF, columnG, columnH, columnI, columnJ, columnK, columnL" +
                "from TableA, TableB, TableC, TableD" +
                "where columnA = columnB and columnC = columnD and columnE = columnF" +
                "and columnG = columnH and columnI = columnJ and columnK = columnL" +
                "group columnA, columnB, columnC");

            LogMessage.Builder logMessage = LogMessage.newBuilder();
            logMessage.addData(value1);
            span.addLogs(logMessage.build());
        }
        return span;
    }

    private SpanObject.Builder createEntrySpan(long startTimestamp, UniqueId.Builder uniqueId, boolean isPrepare,
        ApplicationsStorage.Application providerApplication, int serviceIndex,
        ApplicationsStorage.Application consumerApplication, int instanceIndex) {
        SpanObject.Builder span = SpanObject.newBuilder();
        span.setSpanId(0);
        span.setSpanType(SpanType.Entry);
        span.setSpanLayer(SpanLayer.RPCFramework);
        span.setParentSpanId(-1);
        span.setStartTime(startTimestamp + 500);
        span.setEndTime(startTimestamp + 1500);
        span.setComponentId(ComponentsDefine.DUBBO.getId());
        span.setIsError(false);
        span.addRefs(createReference(uniqueId, isPrepare, consumerApplication, serviceIndex, instanceIndex));

        if (isPrepare) {
            span.setOperationName("org.skywaking.apm.testcase.dubbo.services.GreetService.doBusiness()");
        } else {
            int serviceId = providerApplication.getEntryServiceNames()[serviceIndex].getServiceId();
            span.setOperationNameId(serviceId);
        }
        return span;
    }
}
