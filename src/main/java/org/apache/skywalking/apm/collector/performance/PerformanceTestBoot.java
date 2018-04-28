package org.apache.skywalking.apm.collector.performance;

import io.grpc.*;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.skywalking.apm.collector.client.ClientException;
import org.apache.skywalking.apm.collector.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.apm.collector.performance.register.*;
import org.apache.skywalking.apm.network.proto.SpanType;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class PerformanceTestBoot {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceTestBoot.class);

    static final int APPLICATION_SIZE = 25;
    static final int INSTANCE_SIZE = 10;
    static final int SERVICE_SIZE = 1000;

    public static void main(String[] args) throws ClientException {
        ElasticSearchClient client = new ElasticSearchClient("CollectorDBCluster", true, "10.124.151.1:9300");
        client.initialize();

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 11800).usePlaintext(true).build();

        ApplicationRegister applicationRegister = new ApplicationRegister(channel);
        ApplicationsStorage.Application[] consumerApplications = applicationRegister.register("dubbox-consumer", APPLICATION_SIZE);
        logger.info("consumer application register finish");

        ApplicationsStorage.Application[] providerApplications = applicationRegister.register("dubbox-provider", APPLICATION_SIZE);
        logger.info("provider application register finish");

        ApplicationInstancesRegister instancesRegister = new ApplicationInstancesRegister(channel);
        instancesRegister.register(consumerApplications, INSTANCE_SIZE);
        logger.info("consumer application instance register finish");

        instancesRegister.register(providerApplications, INSTANCE_SIZE);
        logger.info("provider application instance register finish");

        ServiceNameRegister serviceNameRegister = new ServiceNameRegister(channel, client);
        serviceNameRegister.register(consumerApplications, SpanType.Entry_VALUE, "/dubbox-case/case/dubbox-rest/", SERVICE_SIZE);
        logger.info("register entry service name in consumer application finish");

        serviceNameRegister.register(providerApplications, SpanType.Entry_VALUE, "org.skywaking.apm.testcase.dubbo.services.GreetService.doBusiness()", SERVICE_SIZE);
        logger.info("register entry service name in provider application finish");

        serviceNameRegister.register(consumerApplications, SpanType.Exit_VALUE, "org.skywaking.apm.testcase.dubbo.services.GreetService.doBusiness()", SERVICE_SIZE);
        logger.info("register exit service name in consumer application finish");

        serviceNameRegister.warmUp(providerApplications);
        logger.info("provider service name warm up finish");

        serviceNameRegister.warmUp(consumerApplications);
        logger.info("consumer service name warm up finish");

        NetworkRegister networkRegister = new NetworkRegister(channel, client);
        networkRegister.register();
        logger.info("network address register finish");

        networkRegister.warmUp();
        logger.info("network address warm up finish");

        for (ApplicationsStorage.Application consumerApplication : consumerApplications) {
            logger.info("application id: {}, application code: {}", consumerApplication.getApplicationId(), consumerApplication.getApplicationCode());
            for (ApplicationsStorage.Instance instance : consumerApplication.getInstances()) {
                logger.info("instance id: {}, uuid: {}", instance.getInstanceId(), instance.getAgentUUID());
            }
        }

        AtomicLong segmentCounter = new AtomicLong(0);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            new Thread(new TraceSegmentMockRunnable(segmentCounter, providerApplications, consumerApplications, startTime, i)).start();
        }
    }

    static class TraceSegmentMockRunnable implements Runnable {

        private final AtomicLong segmentCounter;
        private final ApplicationsStorage.Application[] providerApplications;
        private final ApplicationsStorage.Application[] consumerApplications;
        private final long startTime;
        private final int num;

        TraceSegmentMockRunnable(AtomicLong segmentCounter,
            ApplicationsStorage.Application[] providerApplications,
            ApplicationsStorage.Application[] consumerApplications, long startTime, int num) {
            this.segmentCounter = segmentCounter;
            this.providerApplications = providerApplications;
            this.consumerApplications = consumerApplications;
            this.startTime = startTime;
            this.num = num;
        }

        @Override public void run() {
            TraceSegmentMock newSegmentMock = new TraceSegmentMock(providerApplications, consumerApplications);
            newSegmentMock.batchMock(num, segmentCounter, startTime);
        }
    }
}
