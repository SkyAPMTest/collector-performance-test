package org.apache.skywalking.apm.collector.performance.register;

import io.grpc.ManagedChannel;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.skywalking.apm.collector.performance.PerformanceBoot;
import org.apache.skywalking.apm.collector.performance.client.*;
import org.apache.skywalking.apm.collector.performance.inventory.*;
import org.apache.skywalking.apm.network.proto.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class NetworkAddressRegister {

    private static final Logger logger = LoggerFactory.getLogger(NetworkAddressRegister.class);

    public static final int MONGO_DB_ADDRESS_ID = 100000 - 1;
    private static final AtomicInteger SEQUENCE = new AtomicInteger(100000);
    private final NetworkAddressRegisterServiceGrpc.NetworkAddressRegisterServiceBlockingStub networkAddressRegisterServiceStub;
    private final ElasticSearchClient client;

    public NetworkAddressRegister(ManagedChannel channel, ElasticSearchClient client) {
        this.networkAddressRegisterServiceStub = PerformanceBoot.attachHeaders(NetworkAddressRegisterServiceGrpc.newBlockingStub(channel));
        this.client = client;
    }

    private static int nextSequence() {
        return SEQUENCE.incrementAndGet();
    }

    public void register(RegisterInventoryStorage.Service[] services) throws IOException {
        save(MONGO_DB_ADDRESS_ID, "RocketMQAddress:2000", 0);

        for (RegisterInventoryStorage.Service service : services) {
            for (int i = 0; i < service.getServiceInstances().length; i++) {
                int id = nextSequence();
                String name = "IPAddress:" + i;

                service.getServiceInstances()[i].setIp(name);
                service.getServiceInstances()[i].setNetworkAddressId(id);
                save(id, name, service.getId());
            }
        }

        RegisterBuilProcessor.flush();

        for (RegisterInventoryStorage.Service service : services) {
            NetworkAddresses.Builder builder = NetworkAddresses.newBuilder();
            RegisterInventoryStorage.ServiceInstance[] serviceInstances = service.getServiceInstances();
            for (RegisterInventoryStorage.ServiceInstance serviceInstance : serviceInstances) {
                builder.addAddresses(serviceInstance.getIp());
            }
            networkAddressRegisterServiceStub.batchRegister(builder.build());
        }
    }

    private void save(int id, String name, int mappingServiceId) throws IOException {
        NetworkAddressInventory inventory = new NetworkAddressInventory();
        inventory.setSequence(id);
        inventory.setName(name);
        inventory.setSrcLayer(2);
        inventory.setRegisterTime(System.currentTimeMillis());
        inventory.setHeartbeatTime(System.currentTimeMillis() + 60 * 60 * 1000);
        RegisterBuilProcessor.add(client.prepareInsert(NetworkAddressInventory.MODEL_NAME, inventory, new NetworkAddressInventory.Builder()));

        ServiceInstanceInventory instanceInventory = new ServiceInstanceInventory();
        instanceInventory.setSequence(id);
        instanceInventory.setName(name);
        instanceInventory.setServiceId(id);
        instanceInventory.setIsAddress(1);
        instanceInventory.setAddressId(id);
        instanceInventory.setRegisterTime(System.currentTimeMillis());
        instanceInventory.setHeartbeatTime(System.currentTimeMillis() + 60 * 60 * 1000);
        RegisterBuilProcessor.add(client.prepareInsert(ServiceInstanceInventory.MODEL_NAME, instanceInventory, new ServiceInstanceInventory.Builder()));

        ServiceInventory serviceInventory = new ServiceInventory();
        serviceInventory.setSequence(id);
        serviceInventory.setName(name);
        serviceInventory.setIsAddress(1);
        serviceInventory.setAddressId(id);
        serviceInventory.setMappingServiceId(mappingServiceId);
        serviceInventory.setMappingLastUpdateTime(System.currentTimeMillis());
        serviceInventory.setRegisterTime(System.currentTimeMillis());
        serviceInventory.setHeartbeatTime(System.currentTimeMillis() + 60 * 60 * 1000);
        RegisterBuilProcessor.add(client.prepareInsert(ServiceInventory.MODEL_NAME, serviceInventory, new ServiceInventory.Builder()));
    }
}
