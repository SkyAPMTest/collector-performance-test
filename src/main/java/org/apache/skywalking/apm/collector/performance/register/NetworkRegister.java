package org.apache.skywalking.apm.collector.performance.register;

import io.grpc.ManagedChannel;
import java.util.*;
import org.apache.skywalking.apm.collector.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.apm.collector.storage.table.register.*;
import org.apache.skywalking.apm.network.proto.*;

/**
 * @author peng-yongsheng
 */
public class NetworkRegister {

    private final NetworkAddressRegisterServiceGrpc.NetworkAddressRegisterServiceBlockingStub networkAddressRegisterServiceBlockingStub;
    private final ElasticSearchClient client;
    public static final int CONSUMER_NETWORK_ID = -1;
    public static final int PROVIDER_NETWORK_ID = 1;

    public NetworkRegister(ManagedChannel channel, ElasticSearchClient client) {
        this.client = client;
        this.networkAddressRegisterServiceBlockingStub = NetworkAddressRegisterServiceGrpc.newBlockingStub(channel);
    }

    public void warmUp() {
        NetworkAddressMappings addressMappings;
        do {
            NetworkAddresses.Builder networkAddresses = NetworkAddresses.newBuilder();
            networkAddresses.addAddresses("172.25.0.4:20880");
            networkAddresses.addAddresses("localhost:27017");
            addressMappings = networkAddressRegisterServiceBlockingStub.batchRegister(networkAddresses.build());
        }
        while (addressMappings.getAddressIdsCount() != 2);
    }

    public void register() {
        Map<String, Object> consumerNetwork = new HashMap<>();
        consumerNetwork.put("na", "172.25.0.4:20880");
        consumerNetwork.put("ni", CONSUMER_NETWORK_ID);
        consumerNetwork.put("ssl", 2);
        consumerNetwork.put("st", 3);
        client.prepareIndex(NetworkAddressTable.TABLE, String.valueOf(CONSUMER_NETWORK_ID)).setSource(consumerNetwork).get();

        Map<String, Object> consumerApplication = new HashMap<>();
        consumerApplication.put("ac", "172.25.0.4:20880");
        int consumerApplicationId = 1000;
        consumerApplication.put("ai", consumerApplicationId);
        consumerApplication.put("ia", 1);
        consumerApplication.put("ni", CONSUMER_NETWORK_ID);
        client.prepareIndex(ApplicationTable.TABLE, String.valueOf(consumerApplicationId)).setSource(consumerApplication).get();

        Map<String, Object> consumerInstance = new HashMap<>();
        int consumerInstanceId = 100000;
        consumerInstance.put("ii", consumerInstanceId);
        consumerInstance.put("iia", 1);
        consumerInstance.put("ac", "172.25.0.4:20880");
        consumerInstance.put("ioi", "");
        consumerInstance.put("iht", 20180428120943L);
        consumerInstance.put("irt", 20180428120947L);
        consumerInstance.put("ai", consumerApplicationId);
        consumerInstance.put("ni", CONSUMER_NETWORK_ID);
        client.prepareIndex(InstanceTable.TABLE, String.valueOf(consumerInstanceId)).setSource(consumerInstance).get();

        Map<String, Object> provider = new HashMap<>();
        provider.put("na", "localhost:27017");
        provider.put("ni", PROVIDER_NETWORK_ID);
        provider.put("ssl", 1);
        provider.put("st", 9);
        client.prepareIndex(NetworkAddressTable.TABLE, String.valueOf(PROVIDER_NETWORK_ID)).setSource(provider).get();

        Map<String, Object> providerApplication = new HashMap<>();
        providerApplication.put("ac", "localhost:27017");
        int providerApplicationId = 1001;
        providerApplication.put("ai", providerApplicationId);
        providerApplication.put("ia", 1);
        providerApplication.put("ni", PROVIDER_NETWORK_ID);
        client.prepareIndex(ApplicationTable.TABLE, String.valueOf(providerApplicationId)).setSource(providerApplication).get();

        Map<String, Object> providerInstance = new HashMap<>();
        int providerInstanceId = 100001;
        providerInstance.put("ii", providerInstanceId);
        providerInstance.put("iia", 1);
        providerInstance.put("ac", "localhost:27017");
        providerInstance.put("ioi", "");
        providerInstance.put("iht", 20180428120943L);
        providerInstance.put("irt", 20180428120947L);
        providerInstance.put("ai", providerApplicationId);
        providerInstance.put("ni", PROVIDER_NETWORK_ID);
        client.prepareIndex(InstanceTable.TABLE, String.valueOf(providerInstanceId)).setSource(providerInstance).get();

        Map<String, Object> target = new HashMap<>();
        target.put("si", -1);
        target.put("ai", providerApplicationId);
        target.put("sn", "mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]");
        target.put("snk", "mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]");
        target.put("sst", 1);
        client.prepareIndex(ServiceNameTable.TABLE, "-1").setSource(target).get();
    }
}
