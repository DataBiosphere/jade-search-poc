package jadesearchpoc.utils;

import jadesearchpoc.application.APIPointers;
import jadesearchpoc.application.Config;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class ElasticSearchUtils {

    private static Logger LOG = LoggerFactory.getLogger(ElasticSearchUtils.class);

    private ElasticSearchUtils() { }

    /**
     * Check the ElasticSearch index to see if a document with the given root_row_id exists.
     * @param indexName the index to search
     * @param rootRowId the root_row_id by which to filter the documents
     * @return the id of the ElasticSearch document that matches the given root_row_id, null if none exists
     */
    public static String findExistingDocumentId(String indexName, String rootRowId) {
        try {
            LOG.info("searching for root_row_id: " + rootRowId + " in index: " + indexName);

            RestHighLevelClient esApi = APIPointers.getElasticsearchApi();
            SearchRequest searchRequest = new SearchRequest("testindex");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("datarepo_rootRowId", rootRowId)
                    .fuzziness(Fuzziness.ZERO);
            searchSourceBuilder.query(matchQueryBuilder);

            // send the request
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = esApi.search(searchRequest, RequestOptions.DEFAULT);
            LOG.trace(DisplayUtils.prettyPrintJson(searchResponse));

            // check for errors
            RestStatus status = searchResponse.status();
            if (!status.equals(RestStatus.OK)) {
                throw new RuntimeException("Error executing ElasticSearch search request");
            }

            // parse the result object to find how many documents matched
            TotalHits totalHits = searchResponse.getHits().getTotalHits();
            if (totalHits == null) {
                throw new RuntimeException("Error parsing ElasticSearch search response");
            }
            long count = totalHits.value;
            LOG.trace("count: " + count);
            if (count == 0) {
                return null; // no document exists
            } else if (count == 1) {
                return searchResponse.getHits().getAt(0).getId(); // document exists, return its id
            } else {

                // bad count, something is wrong
                throw new RuntimeException("Unexpected count (" + count + ") of documents with the same root_row_id");
            }
        } catch (IOException ioEx) {
            LOG.debug(ioEx.getMessage());
            throw new RuntimeException("Error executing ElasticSearch query");
        }
    }

    /**
     * Add an ElasticSearch document to the specified index. Use the documentId if not null,
     * and allow ElasticSearch to generate a new id if it is null.
     * @param indexName the index to add the document to
     * @param documentId the id of the document to update, null to create a new document
     * @param jsonMap the JSON document as a Java Map
     */
    public static void addDocumentToIndex(String indexName, String documentId, Map<String, String> jsonMap) {
        try {
            IndexRequest request = new IndexRequest(indexName);
            if (documentId != null) {
                request.id(documentId);
                LOG.debug("Preserving document id: " + documentId);
            }
            request.source(jsonMap);
            IndexResponse indexResponse = APIPointers.getElasticsearchApi().index(request, RequestOptions.DEFAULT);

            // parse the result object to see if it passed
            String index = indexResponse.getIndex();
            String id = indexResponse.getId();
            LOG.debug("index: " + index + ", id: " + id);
            if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
                LOG.debug("new index document created");
            } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
                LOG.debug("existing index document updated");
            }
        } catch (IOException ioEx) {
            LOG.debug(ioEx.getMessage());
            throw new RuntimeException("Error executing ElasticSearch index request");
        }
    }

    public static void checkClusterHealth() {
        // check cluster status. the ip address here is to the cluster deployed in dev.
        Map<String, String> httpParams = new HashMap<>();
        httpParams.put("wait_for_status", "yellow");

        try {
            Map<String, Object> httpResult = HTTPUtils.sendJavaHttpRequest(
                    "http://" + Config.getElasticSearchIPAddress() + ":"
                            + Config.getElasticSearchPort() + "/_cluster/health",
                    "GET",
                    httpParams);
            LOG.trace(DisplayUtils.prettyPrintJson(httpResult));
        } catch (IOException ioEx) {
            LOG.error(DisplayUtils.buildJsonError("error checking elasticsearch cluster status", ioEx));
        }
    }
}
