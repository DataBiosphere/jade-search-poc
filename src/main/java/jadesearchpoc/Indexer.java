package jadesearchpoc;

import bio.terra.datarepo.model.SnapshotSummaryModel;
import jadesearchpoc.application.APIPointers;
import jadesearchpoc.application.Config;
import jadesearchpoc.utils.DataRepoUtils;
import jadesearchpoc.utils.HTTPUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Indexer {

	public void indexSnapshotByName(String snapshotName) throws Exception {
//		// lookup snapshot id from name
//		SnapshotSummaryModel snapshotSummaryModel = DataRepoUtils.snapshotFromName(snapshotName);
//		if (snapshotSummaryModel == null) {
//			throw new Exception ("snapshot not found");
//		}
////        String json = (new ObjectMapper()).writerWithDefaultPrettyPrinter()
////                .writeValueAsString(snapshotSummaryModel);
////        System.out.println(json);
//
//		// call indexer with the snapshot id
//		String snapshotId = snapshotSummaryModel.getId();
//		APIPointers.getIndexerApi().indexSnapshot(snapshotId);

		indexSnapshot("bb2ea099-d621-42b6-b2b3-faaa95b20849");
		APIPointers.closeElasticsearchApi();
	}

	// single-threaded version
	private void indexSnapshot(String snapshot_id) throws Exception {

		System.out.println("indexing snapshot: " + snapshot_id);

		// check cluster status. the ip address here is to the cluster deployed in dev.
		// curl -X GET "35.232.178.35:9200/_cluster/health?wait_for_status=yellow"
		Map<String, String> httpParams = new HashMap<>();
		httpParams.put("wait_for_status", "yellow");
		Map<String, Object> httpResult = HTTPUtils.sendJavaHttpRequest(
				"http://" + Config.ElasticSearchIPAddress + ":" + Config.ElasticSearchPort + "/_cluster/health",
				"GET",
				httpParams);
		System.out.println(httpResult);

		RestHighLevelClient esApi = APIPointers.getElasticsearchApi();
		System.out.println(esApi);
		SearchRequest searchRequest = new SearchRequest("testindex");
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.matchAllQuery());
		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse = esApi.search(searchRequest, RequestOptions.DEFAULT);
		System.out.println(searchResponse);

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