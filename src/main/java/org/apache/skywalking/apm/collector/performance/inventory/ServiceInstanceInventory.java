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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.*;
import lombok.*;
import org.apache.skywalking.apm.collector.performance.Const;

/**
 * @author peng-yongsheng
 */
public class ServiceInstanceInventory extends RegisterSource {

    public static final String MODEL_NAME = "service_instance_inventory";

    public static final String NAME = "name";
    public static final String SERVICE_ID = "service_id";
    private static final String IS_ADDRESS = "is_address";
    private static final String ADDRESS_ID = "address_id";
    public static final String OS_NAME = "os_name";
    public static final String HOST_NAME = "host_name";
    public static final String PROCESS_NO = "process_no";
    public static final String IPV4S = "ipv4s";
    public static final String LANGUAGE = "language";

    @Setter @Getter private String name = Const.EMPTY_STRING;
    @Setter @Getter private int serviceId;
    @Setter @Getter private int language;
    @Setter @Getter private int isAddress;
    @Setter @Getter private int addressId;
    @Setter @Getter private String osName;
    @Setter @Getter private String hostName;
    @Setter @Getter private int processNo;
    @Setter @Getter private String ipv4s;

    public static String buildId(int serviceId, String serviceInstanceName) {
        return serviceId + Const.ID_SPLIT + serviceInstanceName + Const.ID_SPLIT + BooleanUtils.FALSE + Const.ID_SPLIT + Const.NONE;
    }

    public static String buildId(int serviceId, int addressId) {
        return serviceId + Const.ID_SPLIT + BooleanUtils.TRUE + Const.ID_SPLIT + addressId;
    }

    public String id() {
        if (BooleanUtils.TRUE == isAddress) {
            return buildId(serviceId, addressId);
        } else {
            return buildId(serviceId, name);
        }
    }

    @Override public int hashCode() {
        int result = 17;
        result = 31 * result + serviceId;
        result = 31 * result + name.hashCode();
        result = 31 * result + isAddress;
        result = 31 * result + addressId;
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        ServiceInstanceInventory source = (ServiceInstanceInventory)obj;
        if (serviceId != source.getServiceId())
            return false;
        if (!name.equals(source.getName()))
            return false;
        if (isAddress != source.getIsAddress())
            return false;
        if (addressId != source.getAddressId())
            return false;

        return true;
    }

    public static class Builder implements StorageBuilder<ServiceInstanceInventory> {

        @Override public ServiceInstanceInventory map2Data(Map<String, Object> dbMap) {
            ServiceInstanceInventory inventory = new ServiceInstanceInventory();
            inventory.setSequence((Integer)dbMap.get(SEQUENCE));
            inventory.setServiceId((Integer)dbMap.get(SERVICE_ID));
            inventory.setLanguage((Integer)dbMap.get(LANGUAGE));
            inventory.setIsAddress((Integer)dbMap.get(IS_ADDRESS));
            inventory.setAddressId((Integer)dbMap.get(ADDRESS_ID));
            inventory.setProcessNo((Integer)dbMap.get(PROCESS_NO));

            inventory.setRegisterTime((Long)dbMap.get(REGISTER_TIME));
            inventory.setHeartbeatTime((Long)dbMap.get(HEARTBEAT_TIME));

            inventory.setName((String)dbMap.get(NAME));
            inventory.setOsName((String)dbMap.get(OS_NAME));
            inventory.setHostName((String)dbMap.get(HOST_NAME));
            inventory.setIpv4s((String)dbMap.get(IPV4S));
            return inventory;
        }

        @Override public Map<String, Object> data2Map(ServiceInstanceInventory storageData) {
            Map<String, Object> map = new HashMap<>();
            map.put(SEQUENCE, storageData.getSequence());
            map.put(SERVICE_ID, storageData.getServiceId());
            map.put(LANGUAGE, storageData.getLanguage());
            map.put(IS_ADDRESS, storageData.getIsAddress());
            map.put(ADDRESS_ID, storageData.getAddressId());
            map.put(PROCESS_NO, storageData.getProcessNo());

            map.put(REGISTER_TIME, storageData.getRegisterTime());
            map.put(HEARTBEAT_TIME, storageData.getHeartbeatTime());

            map.put(NAME, storageData.getName());
            map.put(OS_NAME, storageData.getOsName());
            map.put(HOST_NAME, storageData.getHostName());
            map.put(IPV4S, storageData.getIpv4s());
            return map;
        }
    }

    public static class AgentOsInfo {
        @Setter @Getter private String osName;
        @Setter @Getter private String hostname;
        @Setter @Getter private int processNo;
        @Getter private List<String> ipv4s;

        public AgentOsInfo() {
            this.ipv4s = new ArrayList<>();
        }

        public static String ipv4sSerialize(List<String> ipv4) {
            Gson gson = new Gson();
            return gson.toJson(ipv4);
        }

        public static List<String> ipv4sDeserialize(String ipv4s) {
            Gson gson = new Gson();
            return gson.fromJson(ipv4s, new TypeToken<List<String>>() {
            }.getType());
        }
    }
}
