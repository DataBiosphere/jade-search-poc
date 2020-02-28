package jadesearchpoc;

import jadesearchpoc.application.APIPointers;
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

    public void dumpIndex(String indexName) {
        try {
            // build the search source
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            searchSourceBuilder.size(10);

            // execute search
            SearchResponse searchResponse = ElasticSearchUtils.searchAndCheckErrors(indexName, searchSourceBuilder);

            // retrieve results
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                String index = hit.getIndex();
                String id = hit.getId();
                float score = hit.getScore();
                System.out.println("index = " + index + ", id = " + id + ", score = " + score);
                String sourceAsString = hit.getSourceAsString();
                System.out.println(sourceAsString);
            }

            // cleanup
            APIPointers.closeElasticsearchApi();
        } catch (Exception ex) {
            // cleanup
            APIPointers.closeElasticsearchApi();

            throw new RuntimeException(ex);
        }
    }
}
