package jadesearchpoc.command;

import jadesearchpoc.application.APIPointers;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "create-index", description = "create an index with the specified structure",
        subcommands = { CommandLine.HelpCommand.class })
public class CreateIndex implements Callable<Integer> {

    @Parameters(index = "0", description = "index name")
    private String indexName;

    @Option(required = true, names = {"-s", "--structure"}, description = "JSON specifying the index structure")
    private String indexStructure;

    @Override
    public Integer call() throws Exception {
        APIPointers.getIndexerApi().createIndex(indexName, indexStructure);
        return 0;
    }
}
