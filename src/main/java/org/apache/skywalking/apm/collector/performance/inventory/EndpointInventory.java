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

package org.apache.skywalking.apm.collector.performance.inventory;

import java.util.*;
import lombok.*;
import org.apache.skywalking.apm.collector.performance.Const;

/**
 * @author peng-yongsheng
 */
public class EndpointInventory extends RegisterSource {

    public static final String MODEL_NAME = "endpoint_inventory";

    public static final String SERVICE_ID = "service_id";
    public static final String NAME = "name";
    public static final String DETECT_POINT = "detect_point";

    @Setter @Getter private int serviceId;
    @Setter @Getter private String name = Const.EMPTY_STRING;
    @Setter @Getter private int detectPoint;

    public static String buildId(int serviceId, String endpointName) {
        return serviceId + Const.ID_SPLIT + endpointName;
    }

    public String id() {
        return buildId(serviceId, name);
    }

    @Override public int hashCode() {
        int result = 17;
        result = 31 * result + serviceId;
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        EndpointInventory source = (EndpointInventory)obj;
        if (serviceId != source.getServiceId())
            return false;
        if (!name.equals(source.getName()))
            return false;

        return true;
    }

    public static class Builder implements StorageBuilder<EndpointInventory> {

        @Override public EndpointInventory map2Data(Map<String, Object> dbMap) {
            EndpointInventory inventory = new EndpointInventory();
            inventory.setSequence((Integer)dbMap.get(SEQUENCE));
            inventory.setServiceId((Integer)dbMap.get(SERVICE_ID));
            inventory.setName((String)dbMap.get(NAME));
            inventory.setDetectPoint((Integer)dbMap.get(DETECT_POINT));
            inventory.setRegisterTime((Long)dbMap.get(REGISTER_TIME));
            inventory.setHeartbeatTime((Long)dbMap.get(HEARTBEAT_TIME));
            return inventory;
        }

        @Override public Map<String, Object> data2Map(EndpointInventory storageData) {
            Map<String, Object> map = new HashMap<>();
            map.put(SEQUENCE, storageData.getSequence());
            map.put(SERVICE_ID, storageData.getServiceId());
            map.put(NAME, storageData.getName());
            map.put(DETECT_POINT, storageData.getDetectPoint());
            map.put(REGISTER_TIME, storageData.getRegisterTime());
            map.put(HEARTBEAT_TIME, storageData.getHeartbeatTime());
            return map;
        }
    }
}
