package jadesearchpoc.utils;

import bio.terra.datarepo.client.ApiException;
import bio.terra.datarepo.model.EnumerateSnapshotModel;
import bio.terra.datarepo.model.SnapshotSummaryModel;
import jadesearchpoc.application.APIPointers;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class DataRepoUtils {

    /**
     * Lookup a Snapshot by its name.
     * @param snapshotName
     * @return summary model
     */
    public static SnapshotSummaryModel snapshotFromName(String snapshotName) {
        try {
            // call the enumerate snapshots endpoint with a filter on the name
            // note that if there are ever more than 100K snapshots, this could fail incorrectly
            EnumerateSnapshotModel enumerateSnapshot = APIPointers.getRepositoryApi()
                    .enumerateSnapshots(0, 100000, null, null, snapshotName);

            List<SnapshotSummaryModel> studies = enumerateSnapshot.getItems();
            for (SnapshotSummaryModel summary : studies) {
                if (StringUtils.equals(summary.getName(), snapshotName)) {
                    return summary;
                }
            }

            return null;
        } catch (ApiException ex) {
            throw new IllegalArgumentException("Error processing find snapshot by name");
        }
    }
}
