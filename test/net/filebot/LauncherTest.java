package net.filebot;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

/**
 * Tests for the root launcher script (FIX-14 / BUG-06).
 *
 * <p>Verifies that {@code ./filebot} resolves the portable directory
 * dynamically instead of hardcoding a version that may not exist.
 */
public class LauncherTest {

	@Test
	public void launcherDoesNotHardcodeVersion() throws Exception {
		String script = new String(Files.readAllBytes(Path.of("filebot")));
		assertFalse("launcher must not hardcode FileBot_4.8.0-portable", script.contains("4.8.0"));
		assertFalse("launcher must not hardcode any specific version", script.contains("FileBot_4."));
	}

	@Test
	public void launcherResolvesLatestPortableDirectory() throws Exception {
		String script = new String(Files.readAllBytes(Path.of("filebot")));
		assertTrue("launcher must resolve portable directory dynamically", script.contains("FileBot_*-portable") || script.contains("dist/portable"));
	}

	@Test
	public void launcherIsExecutable() throws Exception {
		File launcher = new File("filebot");
		assertTrue("launcher must exist", launcher.isFile());
		assertTrue("launcher must be executable", launcher.canExecute());
	}

	@Test
	public void launcherHasErrorHandling() throws Exception {
		String script = new String(Files.readAllBytes(Path.of("filebot")));
		assertTrue("launcher must report error when no portable build found", script.contains("error") || script.contains("exit 1"));
	}

	@Test
	public void portableBuildCreatesStableSymlink() throws Exception {
		String build = new String(Files.readAllBytes(Path.of("build.xml")));
		assertTrue("ant portable must create dist/portable symlink", build.contains("dist/portable") || build.contains("${dir.dist}/portable"));
	}

}
