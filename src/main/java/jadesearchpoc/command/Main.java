package jadesearchpoc.command;

import picocli.CommandLine;
import picocli.CommandLine.RunAll;
import picocli.CommandLine.Command;

@Command(name = "poc",
		subcommands = { IndexSnapshot.class, CommandLine.HelpCommand.class },
		description = "search/indexing POC for data explorers")
class Main implements Runnable {

	/**
	 * Main entry point into the CLI application.
	 * For picocli, this creates and executes the top-level command Main.
	 * @param args from stdin
	 */
	public static void main(String... args) {
		CommandLine cmd = new CommandLine(new Main());
		cmd.setExecutionStrategy(new RunAll());
		cmd.execute(args);

		if (args.length == 0) { cmd.usage(System.out); }
	}

	@Override
	public void run() { }
}