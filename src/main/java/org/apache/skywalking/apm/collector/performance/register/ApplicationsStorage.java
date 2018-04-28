package org.apache.skywalking.apm.collector.performance.register;

import java.util.*;

/**
 * @author peng-yongsheng
 */
public enum ApplicationsStorage {
    INSTANCE;

    public static class Application {
        private int applicationId;
        private String applicationCode;
        private Instance[] instances;
        private ServiceName[] entryServiceNames = new ServiceName[] {};
        private ServiceName[] exitServiceNames = new ServiceName[] {};
        private Map<String, Integer> entryServiceNameMap = new HashMap<>();
        private Map<String, Integer> exitServiceNameMap = new HashMap<>();

        public int getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(int applicationId) {
            this.applicationId = applicationId;
        }

        public String getApplicationCode() {
            return applicationCode;
        }

        public void setApplicationCode(String applicationCode) {
            this.applicationCode = applicationCode;
        }

        public Instance[] getInstances() {
            return instances;
        }

        public void setInstances(Instance[] instances) {
            this.instances = instances;
        }

        public void resizeEntryServiceName(int size) {
            this.entryServiceNames = new ServiceName[size];
        }

        public void pushEntryServiceName(int index, ServiceName serviceName) {
            this.entryServiceNameMap.put(serviceName.getServiceName(), index);
            this.entryServiceNames[index] = serviceName;
        }

        public void resizeExitServiceName(int size) {
            this.exitServiceNames = new ServiceName[size];
        }

        public void pushExitServiceName(int index, ServiceName serviceName) {
            this.exitServiceNameMap.put(serviceName.getServiceName(), index);
            this.exitServiceNames[index] = serviceName;
        }

        public ServiceName[] getEntryServiceNames() {
            return entryServiceNames;
        }

        public ServiceName[] getExitServiceNames() {
            return exitServiceNames;
        }

        public int indexOfExitServiceName(String serviceName) {
            return this.exitServiceNameMap.get(serviceName);
        }

        public int indexOfEntryServiceName(String serviceName) {
            return this.entryServiceNameMap.get(serviceName);
        }
    }

    public static class Instance {
        private int instanceId;
        private String agentUUID;

        public int getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(int instanceId) {
            this.instanceId = instanceId;
        }

        public String getAgentUUID() {
            return agentUUID;
        }

        public void setAgentUUID(String agentUUID) {
            this.agentUUID = agentUUID;
        }
    }

    public static class ServiceName {
        private int serviceId;
        private String serviceName;
        private boolean warmUp = false;

        public int getServiceId() {
            return serviceId;
        }

        public void setServiceId(int serviceId) {
            this.serviceId = serviceId;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public boolean isWarmUp() {
            return warmUp;
        }

        public void warmUp() {
            this.warmUp = true;
        }
    }
}
