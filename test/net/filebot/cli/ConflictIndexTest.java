package net.filebot.cli;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@code --conflict index} name generation (FIX-18 / BUG-21).
 *
 * <p>Verifies that indexed names are valid for files without extension and
 * folders, and that the 99-attempt limit is removed.
 */
public class ConflictIndexTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private CmdlineOperations ops = new CmdlineOperations();

	@Test
	public void fileWithExtensionGetsValidIndex() throws Exception {
		File base = new File(folder.getRoot(), "movie.mkv");
		File result = ops.nextAvailableIndexedName(base);

		assertEquals("movie.1.mkv", result.getName());
	}

	@Test
	public void fileWithoutExtensionGetsValidIndex() throws Exception {
		File base = new File(folder.getRoot(), "README");
		File result = ops.nextAvailableIndexedName(base);

		assertEquals("README.1", result.getName());
		assertFalse("must not contain .null", result.getName().contains(".null"));
	}

	@Test
	public void folderGetsValidIndex() throws Exception {
		File base = new File(folder.getRoot(), "Season 1");
		File result = ops.nextAvailableIndexedName(base);

		assertEquals("Season 1.1", result.getName());
		assertFalse("must not contain .null", result.getName().contains(".null"));
	}

	@Test
	public void skipsExistingFiles() throws Exception {
		File base = new File(folder.getRoot(), "movie.mkv");
		new File(folder.getRoot(), "movie.1.mkv").createNewFile();
		new File(folder.getRoot(), "movie.2.mkv").createNewFile();

		File result = ops.nextAvailableIndexedName(base);
		assertEquals("movie.3.mkv", result.getName());
	}

	@Test
	public void handlesMoreThan99Conflicts() throws Exception {
		File base = new File(folder.getRoot(), "test.mkv");

		// create 150 conflicting files (would fail with old 99 limit)
		for (int i = 1; i <= 150; i++) {
			new File(folder.getRoot(), "test." + i + ".mkv").createNewFile();
		}

		File result = ops.nextAvailableIndexedName(base);
		assertEquals("test.151.mkv", result.getName());
	}

	@Test
	public void hiddenFileWithDotPrefix() throws Exception {
		File base = new File(folder.getRoot(), ".bashrc");
		File result = ops.nextAvailableIndexedName(base);

		// .bashrc has no extension in the usual sense; getName returns ".bashrc"
		assertFalse("must not contain .null", result.getName().contains(".null"));
	}

}
