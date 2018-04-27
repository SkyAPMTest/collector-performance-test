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

import java.util.concurrent.atomic.AtomicLong;
import org.apache.skywalking.apm.network.proto.UniqueId;

/**
 * @author peng-yongsheng
 */
public enum UniqueIdBuilder {
    INSTANCE;

    private AtomicLong idPart = new AtomicLong(1);

    private final long now = System.currentTimeMillis();

    UniqueId.Builder create() {
        UniqueId.Builder uniqueId = UniqueId.newBuilder();
        long increment = idPart.getAndIncrement();
        uniqueId.addIdParts(now);
        uniqueId.addIdParts(increment);
        uniqueId.addIdParts(increment);
        return uniqueId;
    }
}
