package jadesearchpoc.utils;

import jadesearchpoc.application.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class ElasticSearchUtils {

    private static Logger LOG = LoggerFactory.getLogger(ElasticSearchUtils.class);

    private ElasticSearchUtils() { }

    public static void checkClusterHealth() {
        // check cluster status. the ip address here is to the cluster deployed in dev.
        Map<String, String> httpParams = new HashMap<>();
        httpParams.put("wait_for_status", "yellow");

        try {
            Map<String, Object> httpResult = HTTPUtils.sendJavaHttpRequest(
                    "http://" + Config.ElasticSearchIPAddress + ":" + Config.ElasticSearchPort + "/_cluster/health",
                    "GET",
                    httpParams);
            LOG.trace(DisplayUtils.prettyPrintJson(httpResult));
        } catch (IOException ioEx) {
            LOG.error(DisplayUtils.buildJsonError("error checking elasticsearch cluster status", ioEx));
        }
    }
}
