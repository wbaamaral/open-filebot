package net.filebot;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Revert rename operations in headless mode (e.g. {@code filebot -revert} on a server). Reverted copies and links go to the trash.
 */
public class RevertTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File original;
	File trash;

	@Before
	public void setUp() throws Exception {
		assertTrue("tests must run headless", GraphicsEnvironment.isHeadless());

		original = new File(tmp.newFolder("in"), "Avatar.2009.mkv");
		Files.write(original.toPath(), "video".getBytes(UTF_8));

		trash = new File(System.getProperty("net.filebot.trash.home", new File(tmp.getRoot(), "Trash").getPath()));
	}

	File destination() {
		return new File(tmp.getRoot(), "out/Avatar (2009).mkv");
	}

	File revert(StandardRenameAction action) throws Exception {
		File current = action.rename(original, destination());
		return StandardRenameAction.revert(current, original);
	}

	void assertTrashed(String name) {
		File[] files = new File(trash, "files").listFiles((dir, n) -> n.startsWith(name));
		assertTrue("not in trash: " + name, files != null && files.length > 0);
	}

	@Test
	public void revertCopy() throws Exception {
		assertEquals(original, revert(StandardRenameAction.COPY));
		assertTrue(original.isFile());
		assertFalse(destination().exists());
		assertTrashed("Avatar (2009).mkv");
	}

	@Test
	public void revertHardlink() throws Exception {
		revert(StandardRenameAction.HARDLINK);
		assertTrue(original.isFile());
		assertFalse(destination().exists());
	}

	@Test
	public void revertSymlink() throws Exception {
		revert(StandardRenameAction.SYMLINK);
		assertTrue(original.isFile());
		assertFalse(Files.exists(destination().toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
	}

	@Test
	public void revertMove() throws Exception {
		revert(StandardRenameAction.MOVE);
		assertTrue(original.isFile());
		assertFalse(destination().exists());
	}

}
