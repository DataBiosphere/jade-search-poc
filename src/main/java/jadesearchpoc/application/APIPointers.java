package jadesearchpoc.application;

import bio.terra.datarepo.api.RepositoryApi;
import bio.terra.datarepo.api.ResourcesApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import jadesearchpoc.Indexer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import java.util.UUID;

import java.io.IOException;


/**
 * Singleton container for pointers to the Data Repo API objects
 */
public final class APIPointers {

    private static Logger LOG = LoggerFactory.getLogger(APIPointers.class);

    private static RepositoryApi repositoryApi;
    private static ResourcesApi resourcesApi;
    private static Indexer indexerApi;
    private static RestHighLevelClient elasticsearchApi;
    private static ObjectMapper jacksonObjectMapper;
    private static BigQuery bigQueryApi;

    private APIPointers() { }

    public static RepositoryApi getRepositoryApi() {
        if (repositoryApi == null) {
            repositoryApi = new RepositoryApi();
        }
        Login.checkLogin();
        return repositoryApi;
    }

    public static ResourcesApi getResourcesApi() {
        if (resourcesApi == null) {
            resourcesApi = new ResourcesApi();
        }
        Login.checkLogin();
        return resourcesApi;
    }

    public static Indexer getIndexerApi() {
        if (indexerApi == null) {
            indexerApi = new Indexer();
        }
        return indexerApi;
    }

    public static RestHighLevelClient getElasticsearchApi() {
        if (elasticsearchApi == null) {
            elasticsearchApi = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost(Config.ElasticSearchIPAddress, Config.ElasticSearchPort, "http")));
        }
        return elasticsearchApi;
    }

    public static void closeElasticsearchApi() {
        if (elasticsearchApi == null) {
            return;
        }
        try {
            // free the resources allocated by the ES client (e.g. thread pools)
            elasticsearchApi.close();
        } catch (IOException ioEx) {
            LOG.error("error closing elasticsearch client");
        }
        elasticsearchApi = null;
    }

    public static ObjectMapper getJacksonObjectMapper() {
        if (jacksonObjectMapper == null) {
            jacksonObjectMapper = new ObjectMapper();
        }
        return jacksonObjectMapper;
    }

    public static BigQuery getBigQueryApi() {
        if (bigQueryApi == null) {
            GoogleCredentials googleCredentials = GoogleCredentials.create(
                    new AccessToken(Login.getUserCredential().getAccessToken(), null));
            bigQueryApi = BigQueryOptions.newBuilder()
                    .setProjectId(Config.GoogleDataProjectId)
                    .setCredentials(googleCredentials)
                    .build()
                    .getService();
        }
        return bigQueryApi;
    }
}