package org.apache.skywalking.apm.collector.performance;

import io.grpc.*;
import io.grpc.stub.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.skywalking.apm.collector.performance.client.*;
import org.apache.skywalking.apm.collector.performance.mock.*;
import org.apache.skywalking.apm.collector.performance.register.*;
import org.apache.skywalking.apm.network.proto.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class PerformanceBoot {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceBoot.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        ExecutorService threadPool = Executors.newCachedThreadPool();

        Config config = new Config();
        ElasticSearchClient client = new ElasticSearchClient(config.getEsNodes());
        client.initialize();
        RegisterBuilProcessor.create(client);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(config.getClusterNodes(), 11800).usePlaintext(true).build();

        ServiceInventoryRegister serviceRegister = new ServiceInventoryRegister(channel);
        RegisterInventoryStorage.Service[] serviceAServices = serviceRegister.register("serviceA", config.getServiceNumber(), config.getServiceInstanceNumber());
        logger.info("service A register finish");

        RegisterInventoryStorage.Service[] serviceBServices = serviceRegister.register("serviceB", config.getServiceNumber(), config.getServiceInstanceNumber());
        logger.info("service B register finish");

        RegisterInventoryStorage.Service[] serviceCServices = serviceRegister.register("serviceC", config.getServiceNumber(), config.getServiceInstanceNumber());
        logger.info("service C register finish");

        ServiceInstancesRegister instancesRegister = new ServiceInstancesRegister(channel);
        instancesRegister.register(threadPool, serviceAServices, config.getServiceInstanceNumber());
        logger.info("service A instance register finish");

        instancesRegister.register(threadPool, serviceBServices, config.getServiceInstanceNumber());
        logger.info("service B instance register finish");

        instancesRegister.register(threadPool, serviceCServices, config.getServiceInstanceNumber());
        logger.info("service C instance register finish");

        ServiceAEndpointRegister endpointARegister = new ServiceAEndpointRegister(channel, client);
        endpointARegister.register(serviceAServices, config.getEndpointNumber());
        logger.info("service A's endpoint register finish");

        ServiceBEndpointRegister endpointBRegister = new ServiceBEndpointRegister(channel, client);
        endpointBRegister.register(serviceBServices, config.getEndpointNumber());
        logger.info("service B's endpoint register finish");

        ServiceCEndpointRegister endpointCRegister = new ServiceCEndpointRegister(channel, client);
        endpointCRegister.register(serviceCServices, config.getEndpointNumber());
        logger.info("service C's endpoint register finish");

        TimeUnit.SECONDS.sleep(5);

        NetworkAddressRegister networkAddressARegister = new NetworkAddressRegister(channel, client);
        networkAddressARegister.register(serviceAServices);
        logger.info("service A's network register finish");

        NetworkAddressRegister networkAddressBRegister = new NetworkAddressRegister(channel, client);
        networkAddressBRegister.register(serviceBServices);
        logger.info("service B's network register finish");

        NetworkAddressRegister networkAddressCRegister = new NetworkAddressRegister(channel, client);
        networkAddressCRegister.register(serviceCServices);
        logger.info("service C's network register finish");

        TimeUnit.SECONDS.sleep(5);

        TraceSegmentServiceGrpc.TraceSegmentServiceStub stub = TraceSegmentServiceGrpc.newStub(channel);
        StreamObserver<UpstreamSegment> segmentStreamObserver = PerformanceBoot.attachHeaders(stub).collect(new StreamObserver<Downstream>() {
            @Override public void onNext(Downstream downstream) {
            }

            @Override public void onError(Throwable throwable) {
                logger.error(throwable.getMessage(), throwable);
            }

            @Override public void onCompleted() {
            }
        });

        long startTimestamp = System.currentTimeMillis();
        UniqueId.Builder traceId = UniqueIdBuilder.INSTANCE.create();
        ServiceAMock serviceAMock = new ServiceAMock();
        UniqueId.Builder serviceASegmentId = UniqueIdBuilder.INSTANCE.create();
        serviceAMock.mock(segmentStreamObserver, traceId, serviceASegmentId, startTimestamp, serviceAServices[0], serviceBServices[0], 0, 0);

        ServiceBMock serviceBMock = new ServiceBMock();
        UniqueId.Builder serviceBSegmentId = UniqueIdBuilder.INSTANCE.create();
        serviceBMock.mock(segmentStreamObserver, traceId, serviceBSegmentId, serviceASegmentId, startTimestamp, serviceAServices[0], serviceBServices[0], serviceCServices[0], 0, 0);

        ServiceCMock serviceCMock = new ServiceCMock();
        UniqueId.Builder serviceCSegmentId = UniqueIdBuilder.INSTANCE.create();
        serviceCMock.mock(segmentStreamObserver, traceId, serviceCSegmentId, serviceBSegmentId, startTimestamp, serviceAServices[0], serviceBServices[0], serviceCServices[0], 0, 0);

        AtomicLong segmentCounter = new AtomicLong(0);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 6; i++) {
            new Thread(new TraceSegmentMockRunnable(segmentCounter, config, serviceAServices, serviceCServices, serviceBServices, startTime, i)).start();
        }

        TimeUnit.SECONDS.sleep(5);

        threadPool.shutdown();
        client.shutdown();
        logger.info("Performance test finished.");
    }

    static class TraceSegmentMockRunnable implements Runnable {

        private final Config config;
        private final AtomicLong segmentCounter;
        private final RegisterInventoryStorage.Service[] serviceAServices;
        private final RegisterInventoryStorage.Service[] serviceBServices;
        private final RegisterInventoryStorage.Service[] serviceCServices;
        private final long startTime;
        private final int num;

        TraceSegmentMockRunnable(AtomicLong segmentCounter,
            Config config,
            RegisterInventoryStorage.Service[] serviceAServices,
            RegisterInventoryStorage.Service[] serviceBServices,
            RegisterInventoryStorage.Service[] serviceCServices,
            long startTime, int num) {
            this.config = config;
            this.segmentCounter = segmentCounter;
            this.serviceAServices = serviceAServices;
            this.serviceBServices = serviceBServices;
            this.serviceCServices = serviceCServices;
            this.startTime = startTime;
            this.num = num;
        }

        @Override public void run() {
            TraceSegmentMock newSegmentMock = new TraceSegmentMock(config, serviceAServices, serviceBServices, serviceCServices);
            newSegmentMock.batchMock(num, segmentCounter, startTime);
        }
    }

    public static <T extends AbstractStub<T>> T attachHeaders(T stub) {
        Metadata authHeader = new Metadata();
        authHeader.put(Metadata.Key.of("Authentication", Metadata.ASCII_STRING_MARSHALLER), "tenant1");
        return MetadataUtils.attachHeaders(stub, authHeader);
    }
}
