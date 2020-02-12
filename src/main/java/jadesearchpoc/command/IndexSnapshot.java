package jadesearchpoc.command;

import bio.terra.datarepo.client.ApiException;
import bio.terra.datarepo.model.EnumerateSnapshotModel;
import bio.terra.datarepo.model.SnapshotSummaryModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jadesearchpoc.DataRepoAPIs;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "index-snapshot", description = "index a single snapshot",
        subcommands = { CommandLine.HelpCommand.class })
public class IndexSnapshot implements Callable<Integer> {

    @Parameters(index = "0", description = "snapshot name")
    private String snapshotName;

    @Option(names = {"-u", "--update"}, description = "update existing documents")
    private Boolean update = true;

    @Override
    public Integer call() throws Exception {
        System.out.println("inside IndexSnapshot call()");
        System.out.println("update = " + update);
        System.out.println("snapshot name = " + snapshotName);

        SnapshotSummaryModel snapshotSummaryModel = null;
        try {
            EnumerateSnapshotModel enumerateSnapshot = DataRepoAPIs.getRepositoryApi()
                    .enumerateSnapshots(0, 100000, null, null, snapshotName);

            List<SnapshotSummaryModel> studies = enumerateSnapshot.getItems();
            for (SnapshotSummaryModel summary : studies) {
                if (StringUtils.equals(summary.getName(), snapshotName)) {
                    snapshotSummaryModel = summary;
                    break;
                }
            }

            String json = (new ObjectMapper()).writerWithDefaultPrettyPrinter()
                    .writeValueAsString(snapshotSummaryModel);
            System.out.println(json);
        } catch (ApiException ex) {
            throw new IllegalArgumentException("Error processing find snapshot by name");
        }
        return 0;
    }
}