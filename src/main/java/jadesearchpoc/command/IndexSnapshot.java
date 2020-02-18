package jadesearchpoc.command;

import jadesearchpoc.application.APIPointers;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "index-snapshot", description = "index a single snapshot",
        subcommands = { CommandLine.HelpCommand.class })
public class IndexSnapshot implements Callable<Integer> {

    @Parameters(index = "0", description = "snapshot name")
    private String snapshotName;

    @Option(required = true, names = {"-i", "--index"}, description = "index name")
    private String indexName;

    @Option(required = true, names = {"-b", "--buildIndexCmd"}, description = "build index document command")
    private String buildIndexDocumentCmd;

    @Option(required = true, names = {"-t", "--rootTable"}, description = "root table name")
    private String rootTableName;

    @Option(required = true, names = {"-c", "--rootColumn"}, description = "root column name")
    private String rootColumnName;

    @Option(names = {"-u", "--update"}, description = "update existing documents")
    private Boolean update = true;

    @Override
    public Integer call() throws Exception {
        APIPointers.getIndexerApi().indexSnapshotByName(snapshotName, indexName, buildIndexDocumentCmd,
                rootTableName, rootColumnName, update);
        return 0;
    }
}
