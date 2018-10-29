package org.apache.skywalking.apm.collector.performance.register;

import io.grpc.ManagedChannel;
import java.io.IOException;
import java.util.*;
import org.apache.skywalking.apm.collector.performance.client.ElasticSearchClient;
import org.apache.skywalking.apm.collector.performance.inventory.*;
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

    public void register() throws IOException {
        NetworkAddressInventory networkAddressInventory = new NetworkAddressInventory();
        networkAddressInventory.setName("172.25.0.4:20880");
        networkAddressInventory.setSequence(CONSUMER_NETWORK_ID);
        networkAddressInventory.setSrcLayer(2);
        networkAddressInventory.setRegisterTime(System.currentTimeMillis());
        networkAddressInventory.setHeartbeatTime(System.currentTimeMillis() + 20000);
        client.forceInsert(NetworkAddressInventory.MODEL_NAME, networkAddressInventory, new NetworkAddressInventory.Builder());

        Map<String, Object> consumerApplication = new HashMap<>();
        consumerApplication.put("ac", "172.25.0.4:20880");
        int consumerApplicationId = 1000;
        consumerApplication.put("ai", consumerApplicationId);
        consumerApplication.put("ia", 1);
        consumerApplication.put("ni", CONSUMER_NETWORK_ID);

        ServiceInventory serviceInventory = new ServiceInventory();
        serviceInventory.setName("172.25.0.4:20880");
        serviceInventory.setIsAddress(1);
        serviceInventory.setAddressId(CONSUMER_NETWORK_ID);
        client.forceInsert(ServiceInventory.MODEL_NAME, serviceInventory, new ServiceInventory.Builder());
    }
}
