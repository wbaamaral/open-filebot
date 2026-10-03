package net.filebot;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.File;
import java.util.prefs.Preferences;

import org.junit.Before;
import org.junit.Test;

import net.filebot.util.prefs.FilePreferences;

/**
 * Make sure that tests never read or write the real user data (application folder, temporary files, preferences).
 *
 * The sandbox root is set via {@code -Dnet.filebot.test.root} by {@code ant test}. Running tests without it (e.g. from an IDE) skips these checks.
 */
public class TestEnvironmentTest {

	private File root;

	@Before
	public void setUp() throws Exception {
		String path = System.getProperty("net.filebot.test.root");
		assumeNotNull(path);
		root = new File(path).getCanonicalFile();
	}

	@Test
	public void applicationFoldersInsideSandbox() throws Exception {
		for (ApplicationFolder folder : new ApplicationFolder[] { ApplicationFolder.AppData, ApplicationFolder.Cache, ApplicationFolder.TemporaryFiles }) {
			assertTrue(folder + " => " + folder.get(), isInside(folder.get(), root));
		}
	}

	@Test
	public void trashInsideSandbox() throws Exception {
		assertTrue(isInside(new File(System.getProperty("net.filebot.trash.home")), root));
	}

	@Test
	public void preferencesInsideSandbox() throws Exception {
		assertTrue(Preferences.userRoot() instanceof FilePreferences);
		assertTrue(isInside(new File(System.getProperty("net.filebot.util.prefs.file")), root));
	}

	private static boolean isInside(File file, File folder) throws Exception {
		return file.getCanonicalFile().toPath().startsWith(folder.toPath());
	}

}
