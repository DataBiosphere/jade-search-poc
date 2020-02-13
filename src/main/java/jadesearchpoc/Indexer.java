package jadesearchpoc;

import bio.terra.datarepo.model.SnapshotSummaryModel;
import jadesearchpoc.application.APIPointers;
import jadesearchpoc.utils.DataRepoUtils;
import jadesearchpoc.utils.DisplayUtils;
import jadesearchpoc.utils.ElasticSearchUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Indexer {

	private static Logger LOG = LoggerFactory.getLogger(Indexer.class);

	public void indexSnapshotByName(String snapshotName) throws Exception {
		ElasticSearchUtils.checkClusterHealth();

		// lookup snapshot id from name
		SnapshotSummaryModel snapshotSummaryModel = DataRepoUtils.snapshotFromName(snapshotName);
		if (snapshotSummaryModel == null) {
			throw new Exception ("snapshot not found");
		}
		LOG.trace(DisplayUtils.prettyPrintJson(snapshotSummaryModel));

		// call indexer with the snapshot id
		indexSnapshot(snapshotSummaryModel.getId());

		// cleanup
		APIPointers.closeElasticsearchApi();
	}

	// single-threaded version
	private void indexSnapshot(String snapshot_id) throws Exception {

		LOG.info("indexing snapshot: " + snapshot_id);

		RestHighLevelClient esApi = APIPointers.getElasticsearchApi();
		SearchRequest searchRequest = new SearchRequest("testindex");
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		QueryBuilder filterQuery = QueryBuilders.matchQuery("snapshot_id", snapshot_id);
		FilterAggregationBuilder filter = AggregationBuilders.filter("snapshot_rows", filterQuery);
		MaxAggregationBuilder max = AggregationBuilders.max("max_root_row_id").field("root_row_id");
		filter.subAggregation(max);
		searchSourceBuilder.aggregation(filter);

//		TermsAggregationBuilder aggregation = AggregationBuilders.terms("by_company")
//				.field("company.keyword");
//		aggregation.subAggregation(AggregationBuilders.avg("average_age")
//				.field("age"));
//		searchSourceBuilder.aggregation(aggregation);

//		searchSourceBuilder.query(QueryBuilders.matchAllQuery());
		searchSourceBuilder.size(0);

		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse = esApi.search(searchRequest, RequestOptions.DEFAULT);
		LOG.debug(DisplayUtils.prettyPrintJson(searchResponse));

	// search for the largest root_row_id among all documents with a specific snapshot_id
	//		curl -X POST "35.232.178.35:9200/testindex/_search?size=0&pretty" -H 'Content-Type: application/json' -d'
	//		{
	//			"aggs" : {
	//			"snapshot_rows" : {
	//				"filter" : { "match": { "snapshot_id": "bb2ea099-d621-42b6-b2b3-faaa95b20849" } },
	//				"aggs" : {
	//					"max_root_row_id" : { "max" : { "field" : "root_row_id" } }
	//				}
	//			}
	//		}
	//		}
	//		'

		// fetch elasticsearch highest root_row_id with this snapshot_id
		// if none, set root_row_id to zero

		while (true) {
			break;
			// fetch next highest root_row_id from this snapshot
			// if none, then done
			// update root_row_id with new value

			// call buildIndexDocument(root_row_id)
			// add two fields to the index document: snapshot_id, root_row_id

			// add document to elasticsearch via REST API
		}
	}
}