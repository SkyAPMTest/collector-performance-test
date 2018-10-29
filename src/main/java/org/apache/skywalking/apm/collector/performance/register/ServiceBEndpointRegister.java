package org.apache.skywalking.apm.collector.performance.register;

import io.grpc.ManagedChannel;
import java.io.IOException;
import org.apache.skywalking.apm.collector.performance.client.ElasticSearchClient;
import org.apache.skywalking.apm.network.proto.SpanType;

/**
 * @author peng-yongsheng
 */
public class ServiceBEndpointRegister {

    private final ManagedChannel channel;
    private final ElasticSearchClient client;

    public ServiceBEndpointRegister(ManagedChannel channel, ElasticSearchClient client) {
        this.channel = channel;
        this.client = client;
    }

    public void register(RegisterInventoryStorage.Service[] services, int endpointNumber) throws IOException {
        for (RegisterInventoryStorage.Service service : services) {
            EndpointRegister endpointRegister = new EndpointRegister(channel, client);
            endpointRegister.register(service, SpanType.Entry_VALUE, "org.skywaking.apm.testcase.dubbo.services.GreetService.doBusiness()", endpointNumber);
        }

        for (RegisterInventoryStorage.Service service : services) {
            EndpointRegister endpointRegister = new EndpointRegister(channel, client);
            endpointRegister.register(service, SpanType.Exit_VALUE, "org.apache.skywalking.RocketMQ", endpointNumber);
        }

        warmUp(services);
    }

    private void warmUp(RegisterInventoryStorage.Service[] services) {
        EndpointRegister endpointRegister = new EndpointRegister(channel, client);
        for (RegisterInventoryStorage.Service service : services) {
            if (service.getEntryEndpoints().length > 0) {
                endpointRegister.discovery(service, SpanType.Entry, service.getEntryEndpoints());
            }
            if (service.getExitEndpoints().length > 0) {
                endpointRegister.discovery(service, SpanType.Exit, service.getExitEndpoints());
            }
        }
    }
}
