package jadesearchpoc.command;

import jadesearchpoc.application.Config;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import picocli.CommandLine;
import picocli.CommandLine.RunAll;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParseResult;

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
		// set the logging level from the config
		Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.toLevel(Config.LoggerLevel));

		CommandLine cmd = new CommandLine(new Main())
				.setExecutionExceptionHandler(new PrintExceptionMessageHandler());
		cmd.setExecutionStrategy(new RunAll());
		cmd.execute(args);

		if (args.length == 0) { cmd.usage(System.out); }
	}

	@Override
	public void run() { }

	/**
	 * Custom exception handler class that suppresses the stack trace when an exception is thrown.
	 * Instead, it just prints the exception message and exits the process.
	 */
	private static class PrintExceptionMessageHandler implements CommandLine.IExecutionExceptionHandler {
			public int handleExecutionException(Exception ex,
					CommandLine cmd,
					ParseResult parseResult) {

				cmd.getErr().println(ex.getMessage());

				return cmd.getExitCodeExceptionMapper() != null
						? cmd.getExitCodeExceptionMapper().getExitCode(ex)
						: cmd.getCommandSpec().exitCodeOnExecutionException();
			}
		}
}