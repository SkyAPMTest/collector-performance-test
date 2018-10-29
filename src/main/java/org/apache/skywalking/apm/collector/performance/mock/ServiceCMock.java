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
import org.apache.skywalking.apm.collector.performance.register.*;
import org.apache.skywalking.apm.network.proto.*;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

/**
 * @author peng-yongsheng
 */
public class ServiceCMock {

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
        segment.setApplicationId(serviceCService.getId());

        segment.setApplicationInstanceId(serviceCService.getServiceInstances()[instanceIndex].getId());
        segment.addSpans(createEntrySpan(startTimestamp, parentTraceSegmentId, serviceAService, serviceBService, serviceCService, instanceIndex, endpointIndex));

        for (int i = 1; i <= 20; i++) {
            segment.addSpans(createExitSpan(i, startTimestamp, serviceCService, endpointIndex));
        }

        return segment.build().toByteString();
    }

    private SpanObject.Builder createEntrySpan(long startTimestamp, UniqueId.Builder uniqueId,
        RegisterInventoryStorage.Service serviceAService, RegisterInventoryStorage.Service serviceBService,
        RegisterInventoryStorage.Service serviceCService,
        int instanceIndex, int endpointIndex) {
        SpanObject.Builder span = SpanObject.newBuilder();
        span.setSpanId(0);
        span.setSpanType(SpanType.Entry);
        span.setSpanLayer(SpanLayer.MQ);
        span.setParentSpanId(-1);
        span.setStartTime(startTimestamp + 200);
        span.setEndTime(startTimestamp + 1700);
        span.setComponentId(ComponentsDefine.ROCKET_MQ.getId());
        span.setIsError(false);
        span.addRefs(createReference(uniqueId, serviceAService, serviceBService, serviceCService, instanceIndex, endpointIndex));
        span.setOperationNameId(serviceCService.getEntryEndpoints()[endpointIndex].getId());
        return span;
    }

    private TraceSegmentReference.Builder createReference(UniqueId.Builder parentTraceSegmentId,
        RegisterInventoryStorage.Service serviceAService, RegisterInventoryStorage.Service serviceBService,
        RegisterInventoryStorage.Service serviceCService,
        int instanceIndex, int endpointIndex) {
        TraceSegmentReference.Builder reference = TraceSegmentReference.newBuilder();
        reference.setParentTraceSegmentId(parentTraceSegmentId);
        reference.setParentApplicationInstanceId(serviceBService.getServiceInstances()[instanceIndex].getId());
        reference.setParentSpanId(1);
        reference.setEntryApplicationInstanceId(serviceAService.getServiceInstances()[instanceIndex].getId());
        reference.setRefType(RefType.CrossProcess);

        reference.setParentServiceId(serviceBService.getEntryEndpoints()[endpointIndex].getId());
        reference.setNetworkAddressId(serviceCService.getServiceInstances()[instanceIndex].getNetworkAddressId());
        reference.setEntryServiceId(serviceAService.getEntryEndpoints()[endpointIndex].getId());
        return reference;
    }

    private SpanObject.Builder createExitSpan(int spanId, long startTimestamp,
        RegisterInventoryStorage.Service serviceCService, int endpointIndex) {
        SpanObject.Builder span = SpanObject.newBuilder();
        span.setSpanId(spanId);
        span.setSpanType(SpanType.Exit);
        span.setSpanLayer(SpanLayer.Database);
        span.setParentSpanId(spanId - 1);
        span.setStartTime(startTimestamp + 510);
        span.setEndTime(startTimestamp + 1200);
        span.setComponentId(ComponentsDefine.MONGODB.getId());
        span.setIsError(false);
        span.setOperationNameId(serviceCService.getExitEndpoints()[endpointIndex].getId());
        span.setPeerId(NetworkAddressRegister.MONGO_DB_ADDRESS_ID);

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
}
