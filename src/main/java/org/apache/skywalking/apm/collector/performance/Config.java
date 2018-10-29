package org.apache.skywalking.apm.collector.performance;

import lombok.Getter;

/**
 * @author peng-yongsheng
 */
@Getter
public class Config {
    private final String clusterNodes;
    private final String esNodes;

    private final int serviceNumber;
    private final int serviceInstanceNumber;
    private final int endpointNumber;

    public Config() {
        this.serviceNumber = Integer.parseInt(System.getProperty("service_number", "50"));
        this.serviceInstanceNumber = Integer.parseInt(System.getProperty("service_instance_number", "10"));
        this.endpointNumber = Integer.parseInt(System.getProperty("endpoint_number", "100"));

        this.clusterNodes = System.getProperty("oap_cluster_nodes", "localhost");
        this.esNodes = System.getProperty("es_nodes", "localhost:9200");
    }
}
