package jadesearchpoc.command;

import jadesearchpoc.application.APIPointers;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "search-index", description = "search the documents in a single index",
        subcommands = { CommandLine.HelpCommand.class })
public class SearchIndex implements Callable<Integer> {

    @Parameters(index = "0", description = "index name")
    private String indexName;

    @Option(required = true, names = {"-q", "--query"}, description = "JSON formatted query string")
    private String queryStr;

    @Option(required = false, names = {"-m", "--maxReturned"}, description = "maximum number of records returned")
    private Integer maxReturned;

    @Override
    public Integer call() throws Exception {
        APIPointers.getSearcherApi().searchIndex(indexName, maxReturned, queryStr);
        return 0;
    }
}
