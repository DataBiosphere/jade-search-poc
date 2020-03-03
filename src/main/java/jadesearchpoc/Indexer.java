package jadesearchpoc;

import bio.terra.datarepo.model.SnapshotModel;
import bio.terra.datarepo.model.SnapshotSummaryModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import jadesearchpoc.utils.ElasticSearchUtils;
import jadesearchpoc.utils.ProcessUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Indexer {

    private static Logger LOG = LoggerFactory.getLogger(Indexer.class);

    /**
     * Create or update ElasticSearch index documents for each of the root_row_ids in the snapshot.
     * Note that this method takes the snapshot_name. It is basically a wrapper of the indexSnapshot
     * method below, but includes a call to Data Repo to convert the snapshot name to the snapshot full model.
     * @param snapshotName
     * @param indexName
     * @param buildIndexDocumentCmd
     * @param rootTableName
     * @param rootColumnName this parameter should be eliminated once the datarepo_row_id column is included
     *                       in the snapshot view. then that column should always be used as the rootColumn.
     * @param update true to overwrite existing documents, false to skip them.
     */
    public void indexSnapshotByName(String snapshotName, String indexName, String buildIndexDocumentCmd,
                                    String rootTableName, String rootColumnName, Boolean update) {
        try {
            LOG.info("indexing snapshot (name): " + snapshotName);

            // lookup snapshot id from name
            SnapshotSummaryModel snapshotSummaryModel = DataRepoUtils.snapshotFromName(snapshotName);
            if (snapshotSummaryModel == null) {
                throw new RuntimeException("snapshot not found");
            }
            LOG.trace(DisplayUtils.prettyPrintJson(snapshotSummaryModel));

            // fetch the Snapshot full model to get the data project name
            SnapshotModel snapshotModel = DataRepoUtils.snapshotFromId(snapshotSummaryModel.getId());
            if (snapshotModel == null) {
                throw new RuntimeException("snapshot not found");
            }
            LOG.trace(DisplayUtils.prettyPrintJson(snapshotModel));

            // call indexer with the snapshot id
            indexSnapshot(snapshotModel, indexName, buildIndexDocumentCmd,
                    rootTableName, rootColumnName, update);

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
     * @param snapshot full model, not summary
     * @param indexName
     * @param buildIndexDocumentCmd
     * @param rootTableName
     * @param rootColumnName
     * @param update
     */
    private void indexSnapshot(SnapshotModel snapshot, String indexName, String buildIndexDocumentCmd,
                               String rootTableName, String rootColumnName, Boolean update) throws IOException {
        // fetch all the root_row_ids for this snapshot
        List<String> rootRowIds = getRootRowIdsForSnapshot(snapshot, rootTableName, rootColumnName);

        // loop through all the root_row_ids in this snapshot
        for (int ctr = 0; ctr < rootRowIds.size(); ctr++) {
            String rootRowId = rootRowIds.get(ctr);
            LOG.debug("processing root_row_id: " + rootRowId);

            // check if there exists an ElasticSearch document with this id already
            String documentIdExists = ElasticSearchUtils.findExistingDocumentId(indexName, rootRowId);
            LOG.debug("documentIdExists: " + documentIdExists);

            // if yes and NOT overwriting, then continue
            if (documentIdExists != null && !update.booleanValue()) {
                continue;
            }

            // call user-supplied document generation code
            String jsonStr = buildIndexDocument(snapshot, rootRowId, buildIndexDocumentCmd);
            LOG.debug(jsonStr);

            // add two fields to the index document: snapshot_id, root_row_id
            Map<String, String> jsonMap = addSupplementaryFieldsToDocument(jsonStr, snapshot.getId(), rootRowId);

            // add document to elasticsearch via REST API
            ElasticSearchUtils.addDocumentToIndex(indexName, documentIdExists, jsonMap);
        }
    }

    /**
     * Fetch a list of the root_row_ids for a specific snapshot_id
     * @param snapshot full model to use for filtering the ElasticSearch documents in the index
     * @param rootTableName
     * @param rootColumnName
     * @return the list of the root_row_ids. will be empty if none found
     */
    private List<String> getRootRowIdsForSnapshot(SnapshotModel snapshot, String rootTableName, String rootColumnName) {
        try {
            // build the query to fetch all the root_row_ids from the snapshot
            BigQuery bigquery = APIPointers.getBigQueryApi();
            String queryStr = "SELECT "
                    + rootColumnName + " AS root_column "
                    + "FROM `" + snapshot.getDataProject() + "." + snapshot.getName() + "." + rootTableName + "` "
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

    /**
     * Call the user-supplied ElasticSearch document generation code, passing the snapshot_id
     * and root_row_id as arguments.
     * @param snapshot full model, not summary
     * @param rootRowId
     * @param buildIndexDocumentCmd
     * @return an ElasticSearch document as a JSON-formatted string
     */
    private String buildIndexDocument(SnapshotModel snapshot, String rootRowId, String buildIndexDocumentCmd)
            throws IOException {
        try {
            // the process launcher accepts the command line version split by spaces
            // so split the user-specified command here by spaces
            String[] splitResult = buildIndexDocumentCmd.split(" ");
            if (splitResult.length < 1 || (splitResult.length == 1 && splitResult[0].equals(""))) {
                // this should maybe be an error instead, but for testing purposes, it's helpful
                // to have a default generator without having to define a separate script
                LOG.info("No index document generation command specified, using default.");

                // this code is the default document generation if there is no user-supplied function
                Map<String, Object> jsonMap = new HashMap<>();
                jsonMap.put("date_created", new Date());
                return APIPointers.getJacksonObjectMapper().writeValueAsString(jsonMap);
            }
            String cmd = "";
            List<String> cmdArgs = new ArrayList<>();
            for (int ctr = 0; ctr < splitResult.length; ctr++) {
                if (ctr == 0) {
                    cmd = splitResult[ctr];
                } else {
                    cmdArgs.add(splitResult[ctr]);
                }
            }

            // add the root_row_id, snapshot_id, snapshot_name, snapshot_data_project as arguments to the script.
            // the snapshot name and data_project could be derived from the snapshot id, but since the BigQuery
            // tables use the name and data_project, it's convenient to just pass these fields, too.
            cmdArgs.add(rootRowId);
            cmdArgs.add(snapshot.getId());
            cmdArgs.add(snapshot.getName());
            cmdArgs.add(snapshot.getDataProject());

            // execute the command in a separate process
            List<String> cmdOutput = ProcessUtils.executeCommand(cmd, cmdArgs);

            // join all output lines into a single string
            String jsonStr = String.join("", cmdOutput);
            if (jsonStr.equals("")) {
                // any invalid JSON will throw an error down the line when we try to
                // serialize it back into a map, but checking this case here since it
                // probably means there was a problem calling the user-specified command
                throw new RuntimeException("Empty string generated for index document");
            }
            return jsonStr;
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Error processing JSON");
        }
    }

    /**
     * Add two fields to the index document: snapshot_id, root_row_id
     * This method first attempts to parse the given string into a map, then adds the fields to the map,
     * and then re-serializes it to a string.
     * @param jsonStr ElasticSearch document as a JSON-formatted string
     * @param snapshotId the snapshot_id to add as a supplementary field
     * @param rootRowId the root_row_id to add as a supplementary field
     * @return the updated document as a JSON-formatted string, including the supplementary fields
     */
    private Map<String, String> addSupplementaryFieldsToDocument(String jsonStr, String snapshotId, String rootRowId) {
        try {
            ObjectMapper jacksonMapper = APIPointers.getJacksonObjectMapper();
            Map<String, String> jsonMap = jacksonMapper
                    .readValue(jsonStr, new TypeReference<Map<String, String>>() { });
            jsonMap.put("datarepo_snapshotId", snapshotId);
            jsonMap.put("datarepo_rootRowId", rootRowId);
            return jsonMap;
        } catch (JsonProcessingException jsonEx) {
            throw new RuntimeException("Error processing JSON");
        }
    }

    /**
     * Create a new index with the user-specified index structure, passed as a JSON-formatted string.
     * The structure ]may include the settings, properties, and aliases sections.
     * @param indexName name of the index to create
     * @param indexStructure ElasticSearch index specification as a JSON-formatted string
     */
    public void createIndex(String indexName, String indexStructure) {
        try {
            // build the create index request from the user-provided mapping (as a JSON-formatted string)
            CreateIndexRequest createRequest = new CreateIndexRequest(indexName);
            createRequest.mapping(indexStructure, XContentType.JSON);

            // execute the create index request
            CreateIndexResponse createIndexResponse = APIPointers.getElasticsearchApi().indices()
                    .create(createRequest, RequestOptions.DEFAULT);
            LOG.debug(createIndexResponse.toString());

            // add on the Data Repo-specific mappings
            PutMappingRequest putRequest = new PutMappingRequest(indexName);
            putRequest.source(
                    "{\n" +
                            "  \"properties\": {\n" +
                            "    \"datarepo_snapshotId\": {\n" +
                            "      \"type\": \"keyword\"\n" +
                            "    },\n" +
                            "    \"datarepo_rootRowId\": {\n" +
                            "      \"type\": \"keyword\"\n" +
                            "    }\n" +
                            "  }\n" +
                            "}",
                    XContentType.JSON);

            // execute the put mapping request
            AcknowledgedResponse putMappingResponse = APIPointers.getElasticsearchApi().indices()
                    .putMapping(putRequest, RequestOptions.DEFAULT);
            LOG.debug(putMappingResponse.toString());

            System.out.println("Index created successfully");

            // cleanup
            APIPointers.closeElasticsearchApi();
        } catch (Exception ex) {
            // cleanup
            APIPointers.closeElasticsearchApi();

            throw new RuntimeException(ex);
        }
    }

    /**
     * Delete the specified index.
     * @param indexName the name of the index to delete
     */
    public void deleteIndex(String indexName) {
        try {
            // build the delete index request
            DeleteIndexRequest request = new DeleteIndexRequest(indexName);

            // execute the delete index request
            AcknowledgedResponse deleteIndexResponse = APIPointers.getElasticsearchApi().indices()
                    .delete(request, RequestOptions.DEFAULT);
            LOG.debug(deleteIndexResponse.toString());

            System.out.println("Index deleted successfully");

            // cleanup
            APIPointers.closeElasticsearchApi();
        } catch (Exception ex) {
            // cleanup
            APIPointers.closeElasticsearchApi();

            throw new RuntimeException(ex);
        }
    }

    /**
     * Show the metadata for the specified index. Use _all to show the metadata for all indices.
     * Currently, this method prints the properties and aliases sections of the metadata.
     * @param indexName the name of the index to lookup
     */
    public void showIndex(String indexName) {
        try {
            // build the show index request
            GetIndexRequest request = new GetIndexRequest(indexName);
            request.includeDefaults(false);

            // execute the show index request
            GetIndexResponse getIndexResponse = APIPointers.getElasticsearchApi().indices()
                    .get(request, RequestOptions.DEFAULT);

            // write the response properties and aliases to stdout
            MappingMetaData indexMappings = getIndexResponse.getMappings().get(indexName);
            Map<String, Object> indexTypeMappings = indexMappings.getSourceAsMap();
            System.out.println(DisplayUtils.prettyPrintJson(indexTypeMappings));

            List<AliasMetaData> indexAliases = getIndexResponse.getAliases().get(indexName);
            System.out.println(DisplayUtils.prettyPrintJson(indexAliases));

            // cleanup
            APIPointers.closeElasticsearchApi();
        } catch (Exception ex) {
            // cleanup
            APIPointers.closeElasticsearchApi();

            throw new RuntimeException(ex);
        }
    }
}
