package org.apache.skywalking.apm.collector.performance.register;

import io.grpc.ManagedChannel;
import java.util.UUID;
import org.apache.skywalking.apm.network.proto.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class ApplicationInstancesRegister {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationInstancesRegister.class);

    private final InstanceDiscoveryServiceGrpc.InstanceDiscoveryServiceBlockingStub instanceDiscoveryServiceBlockingStub;

    public ApplicationInstancesRegister(ManagedChannel channel) {
        this.instanceDiscoveryServiceBlockingStub = InstanceDiscoveryServiceGrpc.newBlockingStub(channel);
    }

    public void register(ApplicationsStorage.Application[] applications, int size) {
        for (ApplicationsStorage.Application application : applications) {
            application.setInstances(new ApplicationsStorage.Instance[size]);

            for (int i = 0; i < size; i++) {
                ApplicationInstance.Builder instance = ApplicationInstance.newBuilder();
                instance.setApplicationId(application.getApplicationId());
                String id = UUID.randomUUID().toString();
                instance.setAgentUUID(id);
                instance.setRegisterTime(System.currentTimeMillis());

                OSInfo.Builder osInfo = OSInfo.newBuilder();
                osInfo.setHostname("pengys_" + i);
                osInfo.setOsName("MacOS XX");
                osInfo.setProcessNo(1001);
                osInfo.addIpv4S("10.0.0.3");
                osInfo.addIpv4S("10.0.0.4");
                instance.setOsinfo(osInfo);

                ApplicationInstanceMapping instanceMapping;
                do {
                    instanceMapping = instanceDiscoveryServiceBlockingStub.registerInstance(instance.build());
                    logger.debug("instance id: {}", instanceMapping.getApplicationInstanceId());
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                while (instanceMapping.getApplicationInstanceId() == 0);

                ApplicationsStorage.Instance registeredInstance = new ApplicationsStorage.Instance();
                registeredInstance.setInstanceId(instanceMapping.getApplicationInstanceId());
                registeredInstance.setAgentUUID(id);
                application.getInstances()[i] = registeredInstance;
            }
        }
    }
}
