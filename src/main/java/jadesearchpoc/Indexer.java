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
import org.elasticsearch.client.ml.job.results.Bucket;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Indexer {

	private static Logger LOG = LoggerFactory.getLogger(Indexer.class);

	public void indexSnapshotByName(String snapshotName) throws Exception {
		ElasticSearchUtils.checkClusterHealth();

		// lookup snapshot id from name
//		SnapshotSummaryModel snapshotSummaryModel = DataRepoUtils.snapshotFromName(snapshotName);
//		if (snapshotSummaryModel == null) {
//			throw new Exception ("snapshot not found");
//		}
//		LOG.trace(DisplayUtils.prettyPrintJson(snapshotSummaryModel));

		// call indexer with the snapshot id
		indexSnapshot(snapshotName);
//		indexSnapshot(snapshotSummaryModel.getId());

		// cleanup
		APIPointers.closeElasticsearchApi();
	}

	// single-threaded version
	private void indexSnapshot(String snapshot_id) throws Exception {

		LOG.info("indexing snapshot: " + snapshot_id);

		RestHighLevelClient esApi = APIPointers.getElasticsearchApi();
		SearchRequest searchRequest = new SearchRequest("testindex");
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		// search for the largest root_row_id among all documents with a specific snapshot_id
		//     - build the request object
		QueryBuilder filterQuery = QueryBuilders.matchQuery("snapshot_id", snapshot_id);
		FilterAggregationBuilder filter = AggregationBuilders.filter("snapshot_rows", filterQuery);
		MaxAggregationBuilder max = AggregationBuilders.max("max_root_row_id").field("root_row_id");
		filter.subAggregation(max);
		searchSourceBuilder.aggregation(filter);
		searchSourceBuilder.size(0);

		//     - send the request
		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse = esApi.search(searchRequest, RequestOptions.DEFAULT);
		LOG.trace(DisplayUtils.prettyPrintJson(searchResponse));

		//     - parse the result object to find how many existing documents match the snapshot_id
		Aggregations aggregations = searchResponse.getAggregations();
		Filter snapshotRowsAgg = aggregations.get("snapshot_rows");
		long numSnapshotRows = snapshotRowsAgg.getDocCount();
		LOG.info("num docs found for snapshot: " + numSnapshotRows);

		// if there are no existing documents, set root_row_id to zero
		long rootRowId = 0;
		if (numSnapshotRows > 0) {
			Max maxRootRowIdAgg = snapshotRowsAgg.getAggregations().get("max_root_row_id");
			double maxRootRowId = maxRootRowIdAgg.getValue();
			LOG.debug("max_root_row_id found: " + maxRootRowId);
			rootRowId = (long) maxRootRowId;
		}

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