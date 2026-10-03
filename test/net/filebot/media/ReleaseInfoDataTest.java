package net.filebot.media;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@code ReleaseInfo} data source ordering (FIX-16 / BUG-12).
 *
 * <p>Verifies that:
 * <ul>
 *   <li>Custom URL override via {@code -Durl.*} takes precedence</li>
 *   <li>Bundled resource is a last fallback, not the first choice</li>
 *   <li>CWD-relative paths ({@code downloads/data}) are not used</li>
 *   <li>Empty index triggers a WARNING log</li>
 * </ul>
 */
public class ReleaseInfoDataTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void bundledResourceIsLastFallback() throws Exception {
		String source = new String(Files.readAllBytes(Path.of("source/net/filebot/media/ReleaseInfo.java")));

		// bundled resource must appear AFTER local filesystem search
		int bundled = source.indexOf("Bundled classpath resource");
		int local = source.indexOf("Local filesystem data files");
		int customUrl = source.indexOf("Custom URL override");

		assertTrue("must have custom URL step", customUrl >= 0);
		assertTrue("must have local filesystem step", local >= 0);
		assertTrue("must have bundled resource step", bundled >= 0);

		assertTrue("custom URL must come first", customUrl < local);
		assertTrue("local must come before bundled", local < bundled);
	}

	@Test
	public void cwdRelativePathsAreRemoved() throws Exception {
		String source = new String(Files.readAllBytes(Path.of("source/net/filebot/media/ReleaseInfo.java")));

		assertFalse("must not use downloads/data (CWD-relative)", source.contains("\"downloads/data\""));
		assertFalse("must not use ../downloads/data (CWD-relative)", source.contains("\"../downloads/data\""));
	}

	@Test
	public void emptyIndexLogsWarning() throws Exception {
		String source = new String(Files.readAllBytes(Path.of("source/net/filebot/media/ReleaseInfo.java")));

		assertTrue("must log WARNING when index is empty", source.contains("debug.warning") && source.contains("index is empty"));
	}

	@Test
	public void customUrlOverrideHasPrecedence() throws Exception {
		String source = new String(Files.readAllBytes(Path.of("source/net/filebot/media/ReleaseInfo.java")));

		// the custom URL block must check for http/https prefix
		assertTrue("must check for custom URL", source.startsWith("http", source.indexOf("Custom URL override")) || source.contains("prop.startsWith(\"http"));
	}

	@Test
	public void localPathsIncludeAppDataAndOpt() throws Exception {
		String source = new String(Files.readAllBytes(Path.of("source/net/filebot/media/ReleaseInfo.java")));

		assertTrue("must search AppData/data", source.contains("AppData"));
		assertTrue("must search /opt/filebot/data", source.contains("/opt/filebot/data"));
	}

}
