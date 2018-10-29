/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.collector.performance.client;

import java.io.IOException;
import java.util.*;
import lombok.*;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.skywalking.apm.collector.performance.inventory.*;
import org.elasticsearch.action.admin.indices.create.*;
import org.elasticsearch.action.admin.indices.delete.*;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.*;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class ElasticSearchClient {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchClient.class);

    private static final String TYPE = "type";
    private final String clusterNodes;
    private RestHighLevelClient client;

    public ElasticSearchClient(String clusterNodes) {
        this.clusterNodes = clusterNodes;
    }

    public void initialize() {
        List<HttpHost> pairsList = parseClusterNodes(clusterNodes);

        client = new RestHighLevelClient(
            RestClient.builder(pairsList.toArray(new HttpHost[0])));
    }

    public void shutdown() {
        try {
            client.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private List<HttpHost> parseClusterNodes(String nodes) {
        List<HttpHost> httpHosts = new LinkedList<>();
        logger.info("elasticsearch cluster nodes: {}", nodes);
        String[] nodesSplit = nodes.split(",");
        for (String node : nodesSplit) {
            String host = node.split(":")[0];
            String port = node.split(":")[1];
            httpHosts.add(new HttpHost(host, Integer.valueOf(port)));
        }

        return httpHosts;
    }

    public boolean createIndex(String indexName, Settings settings,
        XContentBuilder mappingBuilder) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(settings);
        request.mapping(TYPE, mappingBuilder);
        CreateIndexResponse response = client.indices().create(request);
        logger.info("create {} index finished, isAcknowledged: {}", indexName, response.isAcknowledged());
        return response.isAcknowledged();
    }

    public boolean deleteIndex(String indexName) throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        DeleteIndexResponse response;
        response = client.indices().delete(request);
        logger.info("delete {} index finished, isAcknowledged: {}", indexName, response.isAcknowledged());
        return response.isAcknowledged();
    }

    public boolean isExistsIndex(String indexName) throws IOException {
        GetIndexRequest request = new GetIndexRequest();
        request.indices(indexName);
        return client.indices().exists(request);
    }

    public SearchResponse search(String indexName, SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.types(TYPE);
        searchRequest.source(searchSourceBuilder);
        return client.search(searchRequest);
    }

    public GetResponse get(String indexName, String id) throws IOException {
        GetRequest request = new GetRequest(indexName, TYPE, id);
        return client.get(request);
    }

    public MultiGetResponse multiGet(String indexName, List<String> ids) throws IOException {
        MultiGetRequest request = new MultiGetRequest();
        ids.forEach(id -> request.add(indexName, TYPE, id));
        return client.multiGet(request);
    }

    public void forceInsert(String modelName, RegisterSource source, StorageBuilder storageBuilder) throws IOException {
        Map<String, Object> objectMap = storageBuilder.data2Map(source);

        XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
        for (String key : objectMap.keySet()) {
            builder.field(key, objectMap.get(key));
        }
        builder.endObject();

        forceInsert(modelName, source.id(), builder);
    }

    public void forceInsert(String indexName, String id, XContentBuilder source) throws IOException {
        IndexRequest request = prepareInsert(indexName, id, source);
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        client.index(request);
    }

    public void forceUpdate(String indexName, String id, XContentBuilder source, long version) throws IOException {
        UpdateRequest request = prepareUpdate(indexName, id, source);
        request.version(version);
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        client.update(request);
    }

    public void forceUpdate(String indexName, String id, XContentBuilder source) throws IOException {
        UpdateRequest request = prepareUpdate(indexName, id, source);
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        client.update(request);
    }

    public IndexRequest prepareInsert(String indexName, String id, XContentBuilder source) {
        return new IndexRequest(indexName, TYPE, id).source(source);
    }

    public IndexRequest prepareInsert(String modelName, RegisterSource source,
        StorageBuilder storageBuilder) throws IOException {
        Map<String, Object> objectMap = storageBuilder.data2Map(source);

        XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
        for (String key : objectMap.keySet()) {
            builder.field(key, objectMap.get(key));
        }
        builder.endObject();

        return prepareInsert(modelName, source.id(), builder);
    }

    public UpdateRequest prepareUpdate(String indexName, String id, XContentBuilder source) {
        return new UpdateRequest(indexName, TYPE, id).doc(source);
    }

    public int delete(String indexName, String timeBucketColumnName, long endTimeBucket) throws IOException {
        Map<String, String> params = Collections.singletonMap("conflicts", "proceed");
        String jsonString = "{" +
            "  \"query\": {" +
            "    \"range\": {" +
            "      \"" + timeBucketColumnName + "\": {" +
            "        \"lte\": " + endTimeBucket +
            "      }" +
            "    }" +
            "  }" +
            "}";
        HttpEntity entity = new NStringEntity(jsonString, ContentType.APPLICATION_JSON);
        Response response = client.getLowLevelClient().performRequest("POST", "/" + indexName + "/_delete_by_query", params, entity);
        return response.getStatusLine().getStatusCode();
    }

    public BulkProcessor createBulkProcessor(int bulkActions, int bulkSize, int flushInterval,
        int concurrentRequests) {
        BulkProcessor.Listener listener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {

            }

            @Override
            public void afterBulk(long executionId, BulkRequest request,
                BulkResponse response) {
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                logger.error("{} data bulk failed, reason: {}", request.numberOfActions(), failure);
            }
        };

        return BulkProcessor.builder(client::bulkAsync, listener)
            .setBulkActions(bulkActions)
            .setBulkSize(new ByteSizeValue(bulkSize, ByteSizeUnit.MB))
            .setFlushInterval(TimeValue.timeValueSeconds(flushInterval))
            .setConcurrentRequests(concurrentRequests)
            .setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), 3))
            .build();
    }

    public static class Feature {
        @Getter @Setter private volatile boolean isFinish = false;
    }
}
