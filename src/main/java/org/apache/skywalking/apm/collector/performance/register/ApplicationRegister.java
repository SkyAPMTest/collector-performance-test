package org.apache.skywalking.apm.collector.performance.register;

import io.grpc.ManagedChannel;
import org.apache.skywalking.apm.network.proto.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class ApplicationRegister {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationRegister.class);

    private final ApplicationRegisterServiceGrpc.ApplicationRegisterServiceBlockingStub applicationRegisterServiceBlockingStub;

    public ApplicationRegister(ManagedChannel channel) {
        this.applicationRegisterServiceBlockingStub = ApplicationRegisterServiceGrpc.newBlockingStub(channel);
    }

    public ApplicationsStorage.Application[] register(String applicationCode, int size) {
        ApplicationsStorage.Application[] applications = new ApplicationsStorage.Application[size];
        for (int i = 0; i < size; i++) {
            Application.Builder application = Application.newBuilder();
            application.setApplicationCode(applicationCode + "_" + i);

            ApplicationMapping applicationMapping;
            do {
                applicationMapping = applicationRegisterServiceBlockingStub.applicationCodeRegister(application.build());
                logger.debug("application id: {}", applicationMapping.getApplication().getValue());
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            while (applicationMapping.getApplication().getValue() == 0);

            ApplicationsStorage.Application registeredApplication = new ApplicationsStorage.Application();
            registeredApplication.setApplicationId(applicationMapping.getApplication().getValue());
            registeredApplication.setApplicationCode(applicationMapping.getApplication().getKey());
            applications[i] = registeredApplication;
        }
        return applications;
    }
}
