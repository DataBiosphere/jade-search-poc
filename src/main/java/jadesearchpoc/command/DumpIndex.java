package jadesearchpoc.command;

import jadesearchpoc.application.APIPointers;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "dump-index", description = "dump all the documents in a single index",
        subcommands = { CommandLine.HelpCommand.class })
public class DumpIndex implements Callable<Integer> {

    @Parameters(index = "0", description = "index name")
    private String indexName;

    @Option(required = false, names = {"-m", "--maxReturned"}, description = "maximum number of records returned")
    private Integer maxReturned;

    @Override
    public Integer call() throws Exception {
        APIPointers.getSearcherApi().dumpIndex(indexName, maxReturned);
        return 0;
    }
}
