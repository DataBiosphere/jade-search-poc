package jadesearchpoc;

import bio.terra.datarepo.model.SnapshotModel;
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
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Indexer {

	private static Logger LOG = LoggerFactory.getLogger(Indexer.class);

	/**
	 * Create or update ElasticSearch index documents for each of the root_row_ids in the snapshot.
	 * Note that this method takes the snapshot_name. It is basically a wrapper of the indexSnapshot
	 * method below, but includes a call to Data Repo to convert the snapshot name to its id.
	 * @param snapshotName
	 * @param rootTableName
	 * @param rootColumnName this parameter should be eliminated once the datarepo_row_id column is included
	 *                       in the snapshot view. then that column should always be used as the rootColumn.
	 */
	public void indexSnapshotByName(String snapshotName, String rootTableName, String rootColumnName) {
		try {
			LOG.info("indexing snapshot (name): " + snapshotName);

			// lookup snapshot id from name
			SnapshotSummaryModel snapshotSummaryModel = DataRepoUtils.snapshotFromName(snapshotName);
			if (snapshotSummaryModel == null) {
				throw new RuntimeException("snapshot not found");
			}
			LOG.trace(DisplayUtils.prettyPrintJson(snapshotSummaryModel));

			// call indexer with the snapshot id
			indexSnapshot(snapshotSummaryModel.getId(), rootTableName, rootColumnName);

			// cleanup
			APIPointers.closeElasticsearchApi();
		} catch (Exception ex) {
			// cleanup
			APIPointers.closeElasticsearchApi();

			throw new RuntimeException(ex);
		}
	}

	/**
	 * Create or update ElasticSearch index documents for each of the root_row_ids in the snapshot.
	 * Note that this method takes the snapshot_id instead of its name.
	 * (single-threaded version)
	 * @param snapshotId
	 * @param rootTableName
	 * @param rootColumnName
	 */
	private void indexSnapshot(String snapshotId, String rootTableName, String rootColumnName) {
		// fetch all the root_row_ids for this snapshot
		List<String> rootRowIds = getRootRowIdsForSnapshot(snapshotId, rootTableName, rootColumnName);

		// loop through all the root_row_ids in this snapshot
		for (int ctr=0; ctr<rootRowIds.size(); ctr++) {
			String rootRowId = rootRowIds.get(ctr);
			LOG.debug("processing root_row_id: " + rootRowId);

			// check if there exists an ElasticSearch document with this id already
			boolean documentExists = checkIfDocumentExists("testindex", rootRowId);
			LOG.debug("documentExists: " + documentExists);

			// if yes and NOT overwriting, then continue

			// call buildIndexDocument(root_row_id)
			// add two fields to the index document: snapshot_id, root_row_id

			// add document to elasticsearch via REST API
		}
	}

	/**
	 * Fetch a list of the root_row_ids for a specific snapshot_id
	 * @param snapshotId by which to filter the ElasticSearch documents in the index
	 * @param rootTableName
	 * @param rootColumnName
	 * @return the list of the root_row_ids. will be empty if none found
	 */
	private List<String> getRootRowIdsForSnapshot(String snapshotId, String rootTableName, String rootColumnName) {
		try {
			// fetch the Snapshot full model to get the data project name
			SnapshotModel snapshotModel = DataRepoUtils.snapshotFromId(snapshotId);
			if (snapshotModel == null) {
				throw new RuntimeException("snapshot not found");
			}
			LOG.trace(DisplayUtils.prettyPrintJson(snapshotModel));
			String snapshotDataProject = snapshotModel.getDataProject();
			String snapshotName = snapshotModel.getName();

			// build the query to fetch all the root_row_ids from the snapshot
			BigQuery bigquery = APIPointers.getBigQueryApi();
			String queryStr = "SELECT "
					+ rootColumnName + " AS root_column "
					+ "FROM `" + snapshotDataProject + "." + snapshotName + "." + rootTableName + "` "
					+ "WHERE " + rootColumnName + " IS NOT NULL "
					+ "ORDER BY " + rootColumnName + " ASC";
			LOG.debug(queryStr);
			QueryJobConfiguration queryConfig =
					QueryJobConfiguration.newBuilder(queryStr)
							.setUseLegacySql(false)
							.build();

			// run query as a job so that it can be re-tried
			JobId jobId = JobId.of(UUID.randomUUID().toString());
			Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

			// block until query returns
			queryJob = queryJob.waitFor();

			// check BigQuery job for errors
			if (queryJob == null) {
				throw new RuntimeException("BigQuery job no longer exists");
			} else if (queryJob.getStatus().getError() != null) {
				// also see queryJob.getStatus().getExecutionErrors() for all errors, not just the latest one
				throw new RuntimeException(queryJob.getStatus().getError().toString());
			}

			// build list from query result object
			TableResult result = queryJob.getQueryResults();
			List<String> rootRowIds = new ArrayList<>();
			for (FieldValueList row : result.iterateAll()) {
				rootRowIds.add(row.get("root_column").getStringValue());
			}
			return rootRowIds;
		} catch (InterruptedException interruptEx) {
			throw new RuntimeException("BigQuery job was interrupted");
		}
	}

	private boolean checkIfDocumentExists(String indexName, String rootRowId) {
		try {
			LOG.info("searching for root_row_id: " + rootRowId + " in index: " + indexName);

			// build the request object
			RestHighLevelClient esApi = APIPointers.getElasticsearchApi();
			CountRequest countRequest = new CountRequest(indexName);
			QueryBuilder countQuery = QueryBuilders.termQuery("root_row_id", rootRowId);
			countRequest.query(countQuery);

			// send the request
			CountResponse countResponse = esApi.count(countRequest, RequestOptions.DEFAULT);
			LOG.trace(DisplayUtils.prettyPrintJson(countResponse));

			// check for errors
			RestStatus status = countResponse.status();
			if (!status.equals(RestStatus.OK)) {
				throw new RuntimeException("Error executing ElasticSearch search request");
			}

			// parse the result object to find how many documents matched
			long count = countResponse.getCount();
			LOG.debug("count: " + count);
			if (count < 0 || count > 1) {
				throw new RuntimeException("Unexpected count (" + count + ") of documents with the same root_row_id");
			}
			return (count == 1);
		} catch (IOException ioEx) {
			LOG.debug(ioEx.getMessage());
			throw new RuntimeException("Error executing ElasticSearch query");
		}
	}

	/**
	 * Search for the largest root_row_id among all documents with a specific snapshot_id
	 * @param snapshotId by which to filter the ElasticSearch documents in the index
	 * @return the highest root_row_id, or the empty string if none found
	 */
	private String getHighestRootRowIdForSnapshot(String snapshotId) throws IOException {
		LOG.info("searching for highest root_row_id for snapshot (id): " + snapshotId);

		// build the request object
		RestHighLevelClient esApi = APIPointers.getElasticsearchApi();
		SearchRequest searchRequest = new SearchRequest("testindex");
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		QueryBuilder filterQuery = QueryBuilders.matchQuery("snapshot_id", snapshotId);
		FilterAggregationBuilder filter = AggregationBuilders.filter("snapshot_rows", filterQuery);
		MaxAggregationBuilder max = AggregationBuilders.max("max_root_row_id").field("root_row_id");
		filter.subAggregation(max);
		searchSourceBuilder.aggregation(filter);
		searchSourceBuilder.size(0);

		// send the request
		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse = esApi.search(searchRequest, RequestOptions.DEFAULT);
		LOG.trace(DisplayUtils.prettyPrintJson(searchResponse));

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
}