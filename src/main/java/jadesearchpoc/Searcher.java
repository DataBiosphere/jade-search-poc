package jadesearchpoc;

import jadesearchpoc.application.APIPointers;
import jadesearchpoc.utils.DisplayUtils;
import jadesearchpoc.utils.ElasticSearchUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Searcher {

    private static Logger LOG = LoggerFactory.getLogger(Searcher.class);

    /**
     * Dump all the documents in the specified index. Optionally, limit the number of records returned.
     * This is a utility method primarily intended for debugging.
     * @param indexName
     * @param maxReturned
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
            System.out.println(DisplayUtils.prettyPrintJson(searchResponse));

            // cleanup
            APIPointers.closeElasticsearchApi();
        } catch (Exception ex) {
            // cleanup
            APIPointers.closeElasticsearchApi();

            throw new RuntimeException(ex);
        }
    }
}
