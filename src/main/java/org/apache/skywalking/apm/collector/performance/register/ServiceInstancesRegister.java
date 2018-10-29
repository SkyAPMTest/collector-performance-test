package org.apache.skywalking.apm.collector.performance.register;

import io.grpc.ManagedChannel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.skywalking.apm.collector.performance.Const;
import org.apache.skywalking.apm.network.proto.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class ServiceInstancesRegister {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstancesRegister.class);

    private AtomicInteger finishedNumber = new AtomicInteger(0);
    private final InstanceDiscoveryServiceGrpc.InstanceDiscoveryServiceBlockingStub instanceDiscoveryServiceBlockingStub;

    public ServiceInstancesRegister(ManagedChannel channel) {
        this.instanceDiscoveryServiceBlockingStub = InstanceDiscoveryServiceGrpc.newBlockingStub(channel);
    }

    public void register(ExecutorService threadPool, RegisterInventoryStorage.Service[] services, int number) {
        for (RegisterInventoryStorage.Service service : services) {
            threadPool.execute(new RegisterRunnable(service, number));
        }

        while (finishedNumber.get() < services.length) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    class RegisterRunnable implements Runnable {

        private final int number;
        private final RegisterInventoryStorage.Service service;

        private RegisterRunnable(RegisterInventoryStorage.Service service, int number) {
            this.service = service;
            this.number = number;
        }

        @Override public void run() {
            boolean unfinished = true;

            while (unfinished) {
                unfinished = false;

                for (int i = 0; i < number; i++) {
                    if (service.getServiceInstances()[i] == null) {
                        ApplicationInstance.Builder instance = ApplicationInstance.newBuilder();
                        instance.setApplicationId(service.getId());
                        instance.setAgentUUID(service.getName() + Const.ID_SPLIT + "Instance" + Const.ID_SPLIT + String.valueOf(i));
                        instance.setRegisterTime(System.currentTimeMillis());

                        OSInfo.Builder osInfo = OSInfo.newBuilder();
                        osInfo.setHostname("pengys_" + i);
                        osInfo.setOsName("MacOS XX");
                        osInfo.setProcessNo(i);
                        osInfo.addIpv4S("10.0.0.3");
                        osInfo.addIpv4S("10.0.0.4");
                        instance.setOsinfo(osInfo);

                        ApplicationInstanceMapping instanceMapping;
                        instanceMapping = instanceDiscoveryServiceBlockingStub.registerInstance(instance.build());
                        if (instanceMapping.getApplicationInstanceId() == 0) {
                            unfinished = true;
                        } else {
                            RegisterInventoryStorage.ServiceInstance registeredServiceInstance = new RegisterInventoryStorage.ServiceInstance();
                            registeredServiceInstance.setId(instanceMapping.getApplicationInstanceId());
                            registeredServiceInstance.setAgentUUID(instance.getAgentUUID());
                            service.getServiceInstances()[i] = registeredServiceInstance;
                        }
                    }
                }

                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            finishedNumber.incrementAndGet();
        }
    }
}
