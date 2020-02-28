package jadesearchpoc.utils;

import com.google.api.client.http.HttpStatusCodes;
import jadesearchpoc.application.APIPointers;
import jadesearchpoc.application.Config;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
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
        LOG.info("searching for root_row_id: " + rootRowId + " in index: " + indexName);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("datarepo_rootRowId", rootRowId)
                .fuzziness(Fuzziness.ZERO);
        searchSourceBuilder.query(matchQueryBuilder);

        // send the request
        SearchResponse searchResponse = searchAndCheckErrors(indexName, searchSourceBuilder);

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

            // check for errors
            RestStatus status = indexResponse.status();
            if (!HttpStatusCodes.isSuccess(status.getStatus())) {
                throw new RuntimeException("Error executing ElasticSearch index request");
            }

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

    public static SearchResponse searchAndCheckErrors(String indexName, SearchSourceBuilder searchSourceBuilder) {
        try {
            // build search request from source
            SearchRequest searchRequest = (indexName ==  null) ? (new SearchRequest()) : (new SearchRequest(indexName));
            searchRequest.source(searchSourceBuilder);

            // execute search
            SearchResponse searchResponse = APIPointers.getElasticsearchApi()
                    .search(searchRequest, RequestOptions.DEFAULT);
            LOG.trace(DisplayUtils.prettyPrintJson(searchResponse));

            // check result for errors
            RestStatus status = searchResponse.status();
            boolean timedOut = searchResponse.isTimedOut();
            if (!HttpStatusCodes.isSuccess(status.getStatus()) || timedOut) {
                LOG.debug(searchResponse.toString());
                throw new RuntimeException("Error executing ElasticSearch search request");
            }

            return searchResponse;
        } catch (IOException ioEx) {
            LOG.debug(ioEx.getMessage());
            throw new RuntimeException("Error executing ElasticSearch search request");
        }
    }

    /**
     * Search for the largest root_row_id among all documents with a specific snapshot_id
     * @param snapshotId by which to filter the ElasticSearch documents in the index
     * @return the highest root_row_id, or the empty string if none found
     */
    public static String getHighestRootRowIdForSnapshot(String snapshotId) throws IOException {
        LOG.info("searching for highest root_row_id for snapshot (id): " + snapshotId);

        // build the search source
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder filterQuery = QueryBuilders.matchQuery("snapshot_id", snapshotId);
        FilterAggregationBuilder filter = AggregationBuilders.filter("snapshot_rows", filterQuery);
        MaxAggregationBuilder max = AggregationBuilders.max("max_root_row_id").field("root_row_id");
        filter.subAggregation(max);
        searchSourceBuilder.aggregation(filter);
        searchSourceBuilder.size(0);

        // send the request
        SearchResponse searchResponse = searchAndCheckErrors("testindex", searchSourceBuilder);

        // parse the result object to find how many existing documents match the snapshot_id
        Aggregations aggregations = searchResponse.getAggregations();
        Filter snapshotRowsAgg = aggregations.get("snapshot_rows");
        long numSnapshotRows = snapshotRowsAgg.getDocCount();
        LOG.info("num docs found for snapshot: " + numSnapshotRows);

        // if there are no existing documents, set root_row_id to empty string
        // empty string should be before all other rows with an ORDER BY row_id clause
        if (numSnapshotRows > 0) {
            Max maxRootRowIdAgg = snapshotRowsAgg.getAggregations().get("max_root_row_id");
            double maxRootRowId = maxRootRowIdAgg.getValue();
            LOG.debug("max_root_row_id found: " + maxRootRowId);
            return Double.toString(maxRootRowId);
        } else {
            LOG.debug("no max_root_row_id found");
            return "";
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
