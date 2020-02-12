package jadesearchpoc;

import jadesearchpoc.application.Config;
import jadesearchpoc.utils.HTTPUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Indexer {

	// single-threaded version
	public void indexSnapshot(String snapshot_id) throws Exception {

		System.out.println("indexing snapshot: " + snapshot_id);

		// check cluster status. the ip address here is to the cluster deployed in dev.
		// curl -X GET "35.232.178.35:9200/_cluster/health?wait_for_status=yellow"
		Map<String, String> httpParams = new HashMap<>();
		httpParams.put("wait_for_status", "yellow");
		Map<String, Object> httpResult = HTTPUtils.sendJavaHttpRequest(
				Config.ElasticSearchIPAddress + ":" + Config.ElasticSearchPort + "/_cluster/health",
				"GET",
				httpParams);
		System.out.println(httpResult);

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