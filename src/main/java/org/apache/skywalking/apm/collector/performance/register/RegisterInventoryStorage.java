package org.apache.skywalking.apm.collector.performance.register;

import java.util.*;
import lombok.*;

/**
 * @author peng-yongsheng
 */
public class RegisterInventoryStorage {

    public static class Service {
        @Getter @Setter private int id;
        @Getter @Setter private String name;
        @Getter private ServiceInstance[] serviceInstances;
        private Endpoint[] entryEndpoints = new Endpoint[] {};
        private Endpoint[] exitEndpoints = new Endpoint[] {};
        private Map<String, Integer> entryEndpointNameMap = new HashMap<>();
        private Map<String, Integer> exitEndpointNameMap = new HashMap<>();

        Service(int instanceNumber) {
            this.serviceInstances = new ServiceInstance[instanceNumber];
        }

        void resizeEntryEndpoint(int size) {
            this.entryEndpoints = new Endpoint[size];
        }

        void pushEntryEndpoint(int index, Endpoint endpoint) {
            this.entryEndpointNameMap.put(endpoint.getName(), index);
            this.entryEndpoints[index] = endpoint;
        }

        void resizeExitEndpoint(int size) {
            this.exitEndpoints = new Endpoint[size];
        }

        void pushExitEndpoint(int index, Endpoint endpoint) {
            this.exitEndpointNameMap.put(endpoint.getName(), index);
            this.exitEndpoints[index] = endpoint;
        }

        public Endpoint[] getEntryEndpoints() {
            return entryEndpoints;
        }

        public Endpoint[] getExitEndpoints() {
            return exitEndpoints;
        }

        int indexOfExitEndpoint(String endpointName) {
            return this.exitEndpointNameMap.get(endpointName);
        }

        int indexOfEntryEndpoint(String endpointName) {
            return this.entryEndpointNameMap.get(endpointName);
        }
    }

    public static class ServiceInstance {
        @Getter @Setter private int id;
        @Getter @Setter private String agentUUID;
        @Getter @Setter private String ip;
        @Getter @Setter private int networkAddressId;
    }

    public static class Endpoint {
        @Getter @Setter private int id;
        @Getter @Setter private String name;
        @Getter @Setter private boolean warmUp = false;
    }
}
