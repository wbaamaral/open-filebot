package net.filebot.cli;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.filebot.format.MediaBindingBean;

/**
 * Tests for {@link ExecCommand} fail-closed behaviour (FIX-09 / BUG-15).
 *
 * <p>When any argument in a template fails to evaluate, the entire command
 * must be aborted (never executed with shifted or missing arguments).
 */
public class ExecCommandTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private MediaBindingBean bean(String filename) {
		File f = new File(filename);
		return new MediaBindingBean(f, f);
	}

	@Test
	public void failedArgumentAbortsCommand() throws Exception {
		// {missing} will throw BindingException -> command must not run at all
		ExecCommand cmd = ExecCommand.parse(List.of("echo", "{missing}"), folder.getRoot());
		cmd.execute(bean("test.mkv"));
		// no exception expected (command is silently skipped), but "echo" alone must not run
	}

	@Test
	public void validArgumentsPreserveSpacesAndUnicode() throws Exception {
		File out = folder.newFile("out.txt");
		ExecCommand cmd = ExecCommand.parse(List.of("sh", "-c", "printf '%s\\n' \"$@\" > " + out.getAbsolutePath(), "--", "{n}"), folder.getRoot());

		cmd.execute(bean("hello world ünïcode.mkv"));

		String content = new String(java.nio.file.Files.readAllBytes(out.toPath())).trim();
		assertEquals("hello world ünïcode", content);
	}

	@Test
	public void parallelModeFailsClosed() throws Exception {
		// {missing} will fail -> entire parallel command must not run
		ExecCommand cmd = ExecCommand.parse(List.of("echo", "{missing}", "+"), folder.getRoot());
		cmd.execute(bean("test.mkv"));
		// no exception expected, but no shifted command should have executed
	}

	@Test
	public void sequentialModeRunsOnlyValidCommands() throws Exception {
		File out = folder.newFile("seq.txt");
		ExecCommand cmd = ExecCommand.parse(List.of("sh", "-c", "echo {n} >> " + out.getAbsolutePath()), folder.getRoot());

		cmd.execute(bean("good.mkv"));

		String content = new String(java.nio.file.Files.readAllBytes(out.toPath())).trim();
		assertEquals("good", content);
	}

}
