package jadesearchpoc.command;

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

    @Option(names = {"-u", "--update"}, description = "update existing documents")
    private Boolean update = true;

    @Override
    public Integer call() throws Exception {
        System.out.println("inside IndexSnapshot call()");
        System.out.println("update = " + update);
        System.out.println("snapshot name = " + snapshotName);
        return 0;
    }
}