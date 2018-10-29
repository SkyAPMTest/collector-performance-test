package org.apache.skywalking.apm.collector.performance.register;

import io.grpc.ManagedChannel;
import java.util.concurrent.TimeUnit;
import org.apache.skywalking.apm.collector.performance.Const;
import org.apache.skywalking.apm.network.proto.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class ServiceInventoryRegister {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInventoryRegister.class);

    private final ApplicationRegisterServiceGrpc.ApplicationRegisterServiceBlockingStub applicationRegisterServiceBlockingStub;

    public ServiceInventoryRegister(ManagedChannel channel) {
        this.applicationRegisterServiceBlockingStub = ApplicationRegisterServiceGrpc.newBlockingStub(channel);
    }

    public RegisterInventoryStorage.Service[] register(String serviceName, int serviceNumber,
        int serviceInstanceNumber) {
        RegisterInventoryStorage.Service[] services = new RegisterInventoryStorage.Service[serviceNumber];

        boolean unfinished = true;
        while (unfinished) {
            unfinished = false;

            for (int i = 0; i < serviceNumber; i++) {
                Application.Builder application = Application.newBuilder();
                application.setApplicationCode(serviceName + Const.ID_SPLIT + i);

                ApplicationMapping applicationMapping;
                applicationMapping = applicationRegisterServiceBlockingStub.applicationCodeRegister(application.build());

                if (applicationMapping.getApplication().getValue() == 0) {
                    unfinished = true;
                }

                RegisterInventoryStorage.Service registeredService = new RegisterInventoryStorage.Service(serviceInstanceNumber);
                registeredService.setId(applicationMapping.getApplication().getValue());
                registeredService.setName(applicationMapping.getApplication().getKey());
                services[i] = registeredService;
            }

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return services;
    }
}
