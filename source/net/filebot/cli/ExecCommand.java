package net.filebot.cli;

import static net.filebot.Logging.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;

import net.filebot.format.ExpressionFormat;
import net.filebot.format.MediaBindingBean;

public class ExecCommand {

	private List<ExpressionFormat> template;
	private boolean parallel;

	private File directory;

	public ExecCommand(List<ExpressionFormat> template, boolean parallel, File directory) {
		this.template = template;
		this.parallel = parallel;
		this.directory = directory;
	}

	public void execute(MediaBindingBean... group) throws IOException, InterruptedException {
		if (parallel) {
			executeParallel(group);
		} else {
			executeSequence(group);
		}
	}

	private void executeSequence(MediaBindingBean... group) throws IOException, InterruptedException {
		// collect unique commands; a command is dropped if any argument fails (fail-closed)
		List<List<String>> commands = new ArrayList<List<String>>();
		for (MediaBindingBean v : group) {
			List<String> command = buildCommand(v);
			if (command != null && !command.isEmpty() && !commands.contains(command)) {
				commands.add(command);
			}
		}

		// execute unique commands; a failure in one command does not interrupt the remaining commands
		for (List<String> command : commands) {
			execute(command);
		}
	}

	private void executeParallel(MediaBindingBean... group) throws IOException, InterruptedException {
		// build single command; fail-closed: any argument failure drops the entire command
		List<String> command = new ArrayList<String>();
		for (ExpressionFormat t : template) {
			List<String> values = new ArrayList<String>();
			for (MediaBindingBean v : group) {
				String value = getArgumentValue(t, v);
				if (value == null) {
					// fail-closed: do not execute a command with missing arguments
					return;
				}
				if (!values.contains(value)) {
					values.add(value);
				}
			}
			command.addAll(values);
		}

		if (!command.isEmpty()) {
			execute(command);
		}
	}

	/**
	 * Build a command from the template and bindings. Returns {@code null} if any
	 * argument fails to evaluate (fail-closed: the command must not be executed
	 * with shifted or missing arguments).
	 */
	private List<String> buildCommand(MediaBindingBean variables) {
		List<String> command = new ArrayList<String>(template.size());
		for (ExpressionFormat t : template) {
			String value = getArgumentValue(t, variables);
			if (value == null) {
				// fail-closed: abort this command entirely
				return null;
			}
			command.add(value);
		}
		return command;
	}

	private String getArgumentValue(ExpressionFormat template, MediaBindingBean variables) {
		try {
			return template.format(variables);
		} catch (Exception e) {
			debug.warning(cause(template.getExpression(), e));
			return null;
		}
	}

	private void execute(List<String> command) throws IOException, InterruptedException {
		ProcessBuilder process = new ProcessBuilder(command);
		process.directory(directory);
		process.inheritIO();

		debug.finest(format("Execute %s", command));

		int exitCode = process.start().waitFor();
		if (exitCode != 0) {
			throw new IOException(String.format("%s failed with exit code %d", command, exitCode));
		}
	}

	public static ExecCommand parse(List<String> args, File directory) throws ScriptException {
		// execute one command per file or one command with many file arguments
		boolean parallel = args.lastIndexOf("+") == args.size() - 1;

		if (parallel) {
			args = args.subList(0, args.size() - 1);
		}

		List<ExpressionFormat> template = new ArrayList<ExpressionFormat>();
		for (String argument : args) {
			template.add(new ExpressionFormat(argument));
		}

		return new ExecCommand(template, parallel, directory);
	}

}
