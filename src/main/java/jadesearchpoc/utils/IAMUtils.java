package jadesearchpoc.utils;

import jadesearchpoc.application.Config;
import jadesearchpoc.application.Login;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.model.ResourceAndAccessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class IAMUtils {

    private static Logger LOG = LoggerFactory.getLogger(IAMUtils.class);

    private static ResourcesApi samResourcesApi;

    private IAMUtils() { }

    /**
     * Lookup in SAM the Snapshot IDs to which the current user has read access.
     * @return a list of snapshot IDs
     */
    public static List<String> getSnapshotIdsForUserSAM() {
        try {
            List<ResourceAndAccessPolicy> resources = listAuthorizedResources("datasnapshot");

            List<String> snapshotIds = new ArrayList<>();

            for (int ctr = 0; ctr < resources.size(); ctr++) {
                ResourceAndAccessPolicy resource = resources.get(ctr);
                if (resource.getAccessPolicyName().equals("reader")) {
                    snapshotIds.add(resource.getResourceId());
                    LOG.debug(resources.get(ctr).getResourceId() + "->" + resources.get(ctr).getAccessPolicyName());
                }
                LOG.trace(resources.get(ctr).getResourceId() + "->" + resources.get(ctr).getAccessPolicyName());
            }

            return snapshotIds;
        } catch (ApiException apiEx) {
            LOG.debug("Error fetching snapshot IDs from SAM");
            throw new RuntimeException(apiEx);
        }
    }

    private static List<ResourceAndAccessPolicy> listAuthorizedResources(String iamResourceType) throws ApiException {
        ResourcesApi samResourcesApi = getSAMResourcesApi();
        return samResourcesApi.listResourcesAndPolicies(iamResourceType);
    }

    private static boolean isAuthorized(String iamResourceType, String resourceId, String action) throws ApiException {
        ResourcesApi samResourcesApi = getSAMResourcesApi();
        boolean authorized = samResourcesApi.resourceAction(iamResourceType, resourceId, action);
        LOG.debug("SAM isAuthorized = " + authorized);
        return authorized;
    }

    private static ResourcesApi getSAMResourcesApi() {
        if (samResourcesApi == null) {
            Login.checkLogin();
            String accessToken = Login.getUserCredential().getAccessToken();
            samResourcesApi = new ResourcesApi(getSAMApiClient(accessToken));
        }
        return samResourcesApi;
    }

    private static ApiClient getSAMApiClient(String accessToken) {
        ApiClient apiClient = new ApiClient();
        apiClient.setAccessToken(accessToken);
        apiClient.setUserAgent("OpenAPI-Generator/1.0.0 java");  // only logs an error in sam
        return apiClient.setBasePath(Config.getSAMIPAddress());
    }
}
