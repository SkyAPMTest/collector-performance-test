package org.apache.skywalking.apm.collector.performance.register;

import io.grpc.ManagedChannel;
import java.util.*;
import org.apache.skywalking.apm.collector.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.apm.collector.core.util.Const;
import org.apache.skywalking.apm.collector.storage.es.dao.register.ServiceNameRegisterEsDAO;
import org.apache.skywalking.apm.collector.storage.table.register.*;
import org.apache.skywalking.apm.network.proto.*;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class ServiceNameRegister {

    private static final Logger logger = LoggerFactory.getLogger(ServiceNameRegister.class);

    private final ServiceNameDiscoveryServiceGrpc.ServiceNameDiscoveryServiceBlockingStub serviceNameDiscoveryServiceBlockingStub;
    private final ServiceNameRegisterEsDAO serviceNameRegisterDAO;
    private final ElasticSearchClient client;
    private int serviceId = 0;

    public ServiceNameRegister(ManagedChannel channel, ElasticSearchClient client) {
        this.serviceNameDiscoveryServiceBlockingStub = ServiceNameDiscoveryServiceGrpc.newBlockingStub(channel);
        this.serviceNameRegisterDAO = new ServiceNameRegisterEsDAO(client);
        this.client = client;
    }

    public void warmUp(ApplicationsStorage.Application[] applications) {
        for (ApplicationsStorage.Application application : applications) {
            if (application.getEntryServiceNames().length > 0) {
                discovery(application, SpanType.Entry, application.getEntryServiceNames());
            }
            if (application.getExitServiceNames().length > 0) {
                discovery(application, SpanType.Exit, application.getExitServiceNames());
            }
        }
    }

    public void register(ApplicationsStorage.Application[] applications, int srcSpanType, String serviceName,
        int size) {
        for (ApplicationsStorage.Application application : applications) {
            if (SpanType.Entry_VALUE == srcSpanType) {
                application.resizeEntryServiceName(size);
            } else {
                application.resizeExitServiceName(size);
            }

            BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (int i = 0; i < size; i++) {
                IndexRequestBuilder indexRequestBuilder = save(i, application, srcSpanType, serviceName + application.getApplicationId() + "-" + i);
                bulkRequest.add(indexRequestBuilder);
            }
            bulkRequest.execute().actionGet();
        }
    }

    private IndexRequestBuilder save(int index, ApplicationsStorage.Application application, int srcSpanType,
        String serviceName) {
        ServiceName newServiceName;

        if (serviceId == 0) {
            ServiceName noneServiceName = new ServiceName();
            noneServiceName.setId("1");
            noneServiceName.setApplicationId(Const.NONE_APPLICATION_ID);
            noneServiceName.setServiceId(Const.NONE_SERVICE_ID);
            noneServiceName.setServiceName(Const.NONE_SERVICE_NAME);
            noneServiceName.setSrcSpanType(Const.SPAN_TYPE_VIRTUAL);
            serviceNameRegisterDAO.save(noneServiceName);

            serviceId = 2;
            newServiceName = new ServiceName();
            newServiceName.setId(String.valueOf(serviceId));
            newServiceName.setApplicationId(application.getApplicationId());
            newServiceName.setServiceId(serviceId);
            newServiceName.setSrcSpanType(srcSpanType);
            newServiceName.setServiceName(serviceName);
        } else {
            serviceId = serviceId + 1;

            newServiceName = new ServiceName();
            newServiceName.setId(String.valueOf(serviceId));
            newServiceName.setApplicationId(application.getApplicationId());
            newServiceName.setServiceId(serviceId);
            newServiceName.setSrcSpanType(srcSpanType);
            newServiceName.setServiceName(serviceName);
        }

        ApplicationsStorage.ServiceName service = new ApplicationsStorage.ServiceName();
        service.setServiceId(serviceId);
        service.setServiceName(serviceName);
        if (SpanType.Entry_VALUE == srcSpanType) {
            application.pushEntryServiceName(index, service);
        } else {
            application.pushExitServiceName(index, service);
        }

        return save(newServiceName);
    }

    private IndexRequestBuilder save(ServiceName serviceName) {
        Map<String, Object> target = new HashMap<>();
        target.put("si", serviceName.getServiceId());
        target.put("ai", serviceName.getApplicationId());
        target.put("sn", serviceName.getServiceName());
        target.put("snk", serviceName.getServiceName());
        target.put("sst", serviceName.getSrcSpanType());
        return client.prepareIndex(ServiceNameTable.TABLE, serviceName.getId()).setSource(target);
    }

    private void discovery(ApplicationsStorage.Application application, SpanType srcSpanType,
        ApplicationsStorage.ServiceName[] serviceNames) {

        while (hasNotWarmUpServiceName(serviceNames)) {
            ServiceNameCollection.Builder serviceNameCollection = builderCollection(application, srcSpanType, serviceNames);

            ServiceNameMappingCollection serviceNameMappingCollection = serviceNameDiscoveryServiceBlockingStub.discovery(serviceNameCollection.build());
            logger.info("warm up service name count {}", serviceNameCollection.getElementsCount());
            for (ServiceNameMappingElement mappingElement : serviceNameMappingCollection.getElementsList()) {
                if (SpanType.Entry.equals(srcSpanType)) {
                    int index = application.indexOfEntryServiceName(mappingElement.getElement().getServiceName());
                    serviceNames[index].warmUp();
                } else {
                    int index = application.indexOfExitServiceName(mappingElement.getElement().getServiceName());
                    serviceNames[index].warmUp();
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private ServiceNameCollection.Builder builderCollection(ApplicationsStorage.Application application,
        SpanType srcSpanType,
        ApplicationsStorage.ServiceName[] serviceNames) {
        ServiceNameCollection.Builder serviceNameCollection = ServiceNameCollection.newBuilder();

        for (ApplicationsStorage.ServiceName serviceName : serviceNames) {
            if (!serviceName.isWarmUp()) {
                ServiceNameElement.Builder serviceNameElement = ServiceNameElement.newBuilder();
                serviceNameElement.setApplicationId(application.getApplicationId());
                serviceNameElement.setServiceName(serviceName.getServiceName());
                serviceNameElement.setSrcSpanType(srcSpanType);
                serviceNameCollection.addElements(serviceNameElement);
            }
        }
        logger.info("need warm up service count: {}", serviceNameCollection.getElementsCount());

        return serviceNameCollection;
    }

    private boolean hasNotWarmUpServiceName(ApplicationsStorage.ServiceName[] serviceNames) {
        boolean warmUp = true;
        for (ApplicationsStorage.ServiceName serviceName : serviceNames) {
            warmUp = warmUp && serviceName.isWarmUp();
        }
        return warmUp;
    }
}
