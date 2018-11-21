package org.apache.skywalking.apm.collector.performance.register;

import io.grpc.ManagedChannel;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.skywalking.apm.collector.performance.*;
import org.apache.skywalking.apm.collector.performance.client.*;
import org.apache.skywalking.apm.collector.performance.inventory.EndpointInventory;
import org.apache.skywalking.apm.network.proto.*;
import org.elasticsearch.action.index.IndexRequest;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class EndpointRegister {

    private static final Logger logger = LoggerFactory.getLogger(EndpointRegister.class);

    private static final AtomicInteger SEQUENCE = new AtomicInteger(1);
    private final ServiceNameDiscoveryServiceGrpc.ServiceNameDiscoveryServiceBlockingStub serviceNameDiscoveryServiceBlockingStub;
    private final ElasticSearchClient client;

    private static int nextSequence() {
        return SEQUENCE.incrementAndGet();
    }

    EndpointRegister(ManagedChannel channel, ElasticSearchClient client) {
        this.serviceNameDiscoveryServiceBlockingStub = PerformanceBoot.attachHeaders(ServiceNameDiscoveryServiceGrpc.newBlockingStub(channel));
        this.client = client;
    }

    public void register(RegisterInventoryStorage.Service service, int srcSpanType, String endpointName,
        int endpointNumber) throws IOException {
        if (SpanType.Entry_VALUE == srcSpanType) {
            service.resizeEntryEndpoint(endpointNumber);
        } else {
            service.resizeExitEndpoint(endpointNumber);
        }

        for (int i = 0; i < endpointNumber; i++) {
            RegisterBuilProcessor.add(save(i, service, srcSpanType, endpointName + Const.ID_SPLIT + i));
        }
        RegisterBuilProcessor.flush();
    }

    private IndexRequest save(int index, RegisterInventoryStorage.Service service, int srcSpanType,
        String endpointName) throws IOException {
        EndpointInventory inventory = new EndpointInventory();
        inventory.setSequence(nextSequence());
        inventory.setName(endpointName);
        inventory.setServiceId(service.getId());
        if (SpanType.Entry_VALUE == srcSpanType) {
            inventory.setDetectPoint(0);
        } else {
            inventory.setDetectPoint(1);
        }
        inventory.setRegisterTime(System.currentTimeMillis());
        inventory.setHeartbeatTime(System.currentTimeMillis() + 60 * 60 * 1000);

        RegisterInventoryStorage.Endpoint endpoint = new RegisterInventoryStorage.Endpoint();
        endpoint.setId(service.getId());
        endpoint.setName(endpointName);
        if (SpanType.Entry_VALUE == srcSpanType) {
            service.pushEntryEndpoint(index, endpoint);
        } else {
            service.pushExitEndpoint(index, endpoint);
        }

        return client.prepareInsert(EndpointInventory.MODEL_NAME, inventory, new EndpointInventory.Builder());
    }

    void discovery(RegisterInventoryStorage.Service service, SpanType srcSpanType,
        RegisterInventoryStorage.Endpoint[] endpoints) {
        ServiceNameCollection.Builder serviceNameCollection = builderCollection(service, srcSpanType, endpoints);

        ServiceNameMappingCollection serviceNameMappingCollection = serviceNameDiscoveryServiceBlockingStub.discovery(serviceNameCollection.build());
        for (ServiceNameMappingElement mappingElement : serviceNameMappingCollection.getElementsList()) {
            if (SpanType.Entry.equals(srcSpanType)) {
                int index = service.indexOfEntryEndpoint(mappingElement.getElement().getServiceName());
                endpoints[index].setWarmUp(true);
            } else {
                int index = service.indexOfExitEndpoint(mappingElement.getElement().getServiceName());
                endpoints[index].setWarmUp(true);
            }
        }
    }

    private ServiceNameCollection.Builder builderCollection(RegisterInventoryStorage.Service service,
        SpanType srcSpanType,
        RegisterInventoryStorage.Endpoint[] endpoints) {
        ServiceNameCollection.Builder serviceNameCollection = ServiceNameCollection.newBuilder();

        for (RegisterInventoryStorage.Endpoint endpoint : endpoints) {
            if (!endpoint.isWarmUp()) {
                ServiceNameElement.Builder serviceNameElement = ServiceNameElement.newBuilder();
                serviceNameElement.setApplicationId(service.getId());
                serviceNameElement.setServiceName(endpoint.getName());
                serviceNameElement.setSrcSpanType(srcSpanType);
                serviceNameCollection.addElements(serviceNameElement);
            }
        }

        return serviceNameCollection;
    }
}
