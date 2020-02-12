package jadesearchpoc.application;

import bio.terra.datarepo.api.RepositoryApi;
import bio.terra.datarepo.api.ResourcesApi;
import jadesearchpoc.Indexer;


/**
 * Singleton container for pointers to the Data Repo API objects
 */
public final class APIPointers {
    private static RepositoryApi repositoryApi;
    private static ResourcesApi resourcesApi;
    private static Indexer indexerApi;

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
}
