package jadesearchpoc.command;

import bio.terra.datarepo.model.SnapshotSummaryModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jadesearchpoc.Indexer;
import jadesearchpoc.application.APIPointers;
import jadesearchpoc.utils.DataRepoUtils;
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
        // lookup snapshot id from name
        SnapshotSummaryModel snapshotSummaryModel = DataRepoUtils.snapshotFromName(snapshotName);
        if (snapshotSummaryModel == null) {
            throw new Exception ("snapshot not found");
        }

//        String json = (new ObjectMapper()).writerWithDefaultPrettyPrinter()
//                .writeValueAsString(snapshotSummaryModel);
//        System.out.println(json);

        // call indexer with the snapshot id
        String snapshotId = snapshotSummaryModel.getId();
        APIPointers.getIndexerApi().indexSnapshot(snapshotId);

        return 0;
    }
}