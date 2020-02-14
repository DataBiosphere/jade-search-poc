package jadesearchpoc;

import bio.terra.datarepo.model.SnapshotSummaryModel;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import jadesearchpoc.application.APIPointers;
import jadesearchpoc.utils.DataRepoUtils;
import jadesearchpoc.utils.DisplayUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
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

import java.util.UUID;

public class Indexer {

	private static Logger LOG = LoggerFactory.getLogger(Indexer.class);

	public void indexSnapshotByName(String snapshotName) throws Exception {
		LOG.info("indexing snapshot (name): " + snapshotName);

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

	private void indexSnapshot(String snapshotId) throws Exception {
		LOG.info("indexing snapshot (id): " + snapshotId);

		RestHighLevelClient esApi = APIPointers.getElasticsearchApi();
		SearchRequest searchRequest = new SearchRequest("testindex");
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		// search for the largest root_row_id among all documents with a specific snapshot_id
		//     - build the request object
		QueryBuilder filterQuery = QueryBuilders.matchQuery("snapshot_id", snapshotId);
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
		long rootRowId = -1;
		if (numSnapshotRows > 0) {
			Max maxRootRowIdAgg = snapshotRowsAgg.getAggregations().get("max_root_row_id");
			double maxRootRowId = maxRootRowIdAgg.getValue();
			LOG.debug("max_root_row_id found: " + maxRootRowId);
			rootRowId = (long) maxRootRowId;
		}
		LOG.info("highest root_row_id found: " + rootRowId);

		// single-threaded version
		while (true) {
			// fetch next highest root_row_id from this snapshot
			getNextHighestRootRowId();
			// if none, then done
			// update root_row_id with new value

			// call buildIndexDocument(root_row_id)
			// add two fields to the index document: snapshot_id, root_row_id

			// add document to elasticsearch via REST API

			break;
		}
	}

	private String getNextHighestRootRowId() {
		try {
			BigQuery bigquery = APIPointers.getBigQueryApi();

			QueryJobConfiguration queryConfig =
					QueryJobConfiguration.newBuilder(
							"SELECT "
									+ "CONCAT('https://stackoverflow.com/questions/', CAST(id as STRING)) as url, "
									+ "view_count "
									+ "FROM `bigquery-public-data.stackoverflow.posts_questions` "
									+ "WHERE tags like '%google-bigquery%' "
									+ "ORDER BY favorite_count DESC LIMIT 10")
							// Use standard SQL syntax for queries.
							// See: https://cloud.google.com/bigquery/sql-reference/
							.setUseLegacySql(false)
							.build();

			// Create a job ID so that we can safely retry.
			JobId jobId = JobId.of(UUID.randomUUID().toString());
			Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

			// Wait for the query to complete.
			queryJob = queryJob.waitFor();

			// Check for errors
			if (queryJob == null) {
				throw new RuntimeException("BigQuery job no longer exists");
			} else if (queryJob.getStatus().getError() != null) {
				// You can also look at queryJob.getStatus().getExecutionErrors() for all
				// errors, not just the latest one.
				throw new RuntimeException(queryJob.getStatus().getError().toString());
			}

			// Get the results.
			TableResult result = queryJob.getQueryResults();

			// Print all pages of the results.
			for (FieldValueList row : result.iterateAll()) {
				String url = row.get("url").getStringValue();
				long viewCount = row.get("view_count").getLongValue();
				System.out.printf("url: %s views: %d%n", url, viewCount);
			}
		} catch (InterruptedException interruptEx) {
			throw new RuntimeException("BigQuery job was interrupted");
		}

		return "mariko";
	}
}