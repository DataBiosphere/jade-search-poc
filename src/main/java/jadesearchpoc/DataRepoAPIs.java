package jadesearchpoc;

import bio.terra.datarepo.api.RepositoryApi;
import bio.terra.datarepo.api.ResourcesApi;

/**
 * Singleton container for pointers to the Data Repo API objects
 */
public final class DataRepoAPIs {
    private static RepositoryApi repositoryApi;
    private static ResourcesApi resourcesApi;

    private DataRepoAPIs() { }

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
}
