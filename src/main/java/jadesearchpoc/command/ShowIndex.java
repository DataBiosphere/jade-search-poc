package jadesearchpoc.command;

import jadesearchpoc.application.APIPointers;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "show-index", description = "show the metadata for an index",
        subcommands = { CommandLine.HelpCommand.class })
public class ShowIndex implements Callable<Integer> {

    @Parameters(index = "0", description = "index name")
    private String indexName;

    @Override
    public Integer call() throws Exception {
        APIPointers.getIndexerApi().showIndex(indexName);
        return 0;
    }
}
