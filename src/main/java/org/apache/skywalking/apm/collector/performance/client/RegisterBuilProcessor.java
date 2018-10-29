package org.apache.skywalking.apm.collector.performance.client;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

/**
 * @author peng-yongsheng
 */
public class RegisterBuilProcessor {

    private static BulkProcessor bulkProcessor;

    public static void create(ElasticSearchClient client) {
        bulkProcessor = client.createBulkProcessor(2000, 20, 10, 2);
    }

    public static void add(IndexRequest indexRequest) {
        bulkProcessor.add(indexRequest);
    }

    public static void flush() {
        bulkProcessor.flush();
    }
}
