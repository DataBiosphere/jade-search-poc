package jadesearchpoc;

import jadesearchpoc.application.APIPointers;
import jadesearchpoc.utils.DisplayUtils;
import jadesearchpoc.utils.ElasticSearchUtils;
import jadesearchpoc.utils.IAMUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class Searcher {

    private static Logger LOG = LoggerFactory.getLogger(Searcher.class);

    /**
     * Execute the user's query on the specified index. Documents that the user does not have access to
     * are filtered out before the query is performed. This is accomplished by embedding the user's query
     * in the must clause of a compound boolean query, and specifying the filter clause to only return
     * documents with the snapshotIds that the user has access to.
     * @param indexName the index to search
     * @param maxReturned the maximum number of results to return
     * @param queryStr the user-specified query string
     */
    public void searchIndex(String indexName, Integer maxReturned, String queryStr) {
        try {
            // parse the JSON formatted user-specified query string
            // use a SearchSourceBuilder to turn the queryStr into a QueryBuilder
            SearchSourceBuilder userQuerySource = new SearchSourceBuilder();
            SearchModule searchModule = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
            try (XContentParser parser = XContentFactory.xContent(XContentType.JSON)
                    .createParser(new NamedXContentRegistry(searchModule.getNamedXContents()),
                            DeprecationHandler.THROW_UNSUPPORTED_OPERATION, queryStr)) {
                userQuerySource.parseXContent(parser);
            }

            // build a match query on the snapshotId
            List<String> snapshotIdsList = IAMUtils.getSnapshotIdsForUserSAM();
            TermsQueryBuilder filterQuery = QueryBuilders.termsQuery("datarepo_snapshotId", snapshotIdsList);

            // build a compound boolean query with the user-specified query as the must clause
            // and the filter query as the snapshotId filter built above.
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.must(userQuerySource.query());
            boolQuery.filter(filterQuery);

            // update the query to be the wrapped query built above
            userQuerySource.query(boolQuery);
            // note the reason we don't build a new query source here is because re-using the existing one
            // preserves any aggregations the user specified without having to parse and re-set them in the source

            // need to set top-level search parameters, such as size, on the compound query, not the user's query
            // other top-level parameters are here:
            // https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-body.html
            if (maxReturned != null) {
                userQuerySource.size(maxReturned);
            }
            LOG.debug(userQuerySource.query().toString());
            if (userQuerySource.aggregations() != null) {
                LOG.debug(userQuerySource.aggregations().toString());
            }

            // execute search
            SearchResponse searchResponse = ElasticSearchUtils.searchAndCheckErrors(indexName, userQuerySource);

            // print response to stdout
            // (don't call Jackson to pretty-print here because it doesn't handle arrays of objects well)
            LOG.info(searchResponse.toString());

            // cleanup
            APIPointers.closeElasticsearchApi();
        } catch (Exception ex) {
            // cleanup
            APIPointers.closeElasticsearchApi();

            throw new RuntimeException(ex);
        }
    }

    /**
     * Dump all the documents in the specified index. Optionally, limit the number of records returned.
     * Note that this method does NOT enforce snapshot access control. It is a utility method intended
     * primarily for debugging.
     * @param indexName the index to dump
     * @param maxReturned the maximum number of results to return
     */
    public void dumpIndex(String indexName, Integer maxReturned) {
        try {
            // build the search source
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            if (maxReturned != null) {
                searchSourceBuilder.size(maxReturned);
            }

            // execute search
            SearchResponse searchResponse = ElasticSearchUtils.searchAndCheckErrors(indexName, searchSourceBuilder);

            // retrieve results
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                String index = hit.getIndex();
                String id = hit.getId();
                float score = hit.getScore();
                LOG.trace("index = " + index + ", id = " + id + ", score = " + score);
                String sourceAsString = hit.getSourceAsString();
                LOG.trace(sourceAsString);
            }
            LOG.info(DisplayUtils.prettyPrintJson(searchResponse));

            // cleanup
            APIPointers.closeElasticsearchApi();
        } catch (Exception ex) {
            // cleanup
            APIPointers.closeElasticsearchApi();

            throw new RuntimeException(ex);
        }
    }
}
