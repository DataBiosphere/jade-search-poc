package jadesearchpoc.application;

import bio.terra.datarepo.api.RepositoryApi;
import bio.terra.datarepo.api.ResourcesApi;
import jadesearchpoc.Indexer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;


/**
 * Singleton container for pointers to the Data Repo API objects
 */
public final class APIPointers {
    private static RepositoryApi repositoryApi;
    private static ResourcesApi resourcesApi;
    private static Indexer indexerApi;
    private static RestHighLevelClient elasticsearchApi;

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

        // free the resources allocated by the ES client (e.g. thread pools)
        try {
            elasticsearchApi.close();
        } catch (IOException ioEx) {
            System.out.println("error closing elasticsearch client");
        }

        elasticsearchApi = null;
    }
}