package jadesearchpoc;

public class Indexer {

	// single-threaded version
	public void indexSnapshot(String snapshot_id) {

		// fetch elasticsearch highest root_row_id with this snapshot_id
		// if none, set root_row_id to zero

		while (true) {
			// fetch next highest root_row_id from this snapshot
			// if none, then done
			// update root_row_id with new value

			// call buildIndexDocument(root_row_id)
			// add two fields to the index document: snapshot_id, root_row_id

			// add document to elasticsearch via REST API
		}
	}
}