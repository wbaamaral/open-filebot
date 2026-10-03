package net.filebot.cli;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.filebot.StandardRenameAction;

/**
 * Tests for {@code --apply date} post-processing (FIX-15 / BUG-09).
 *
 * <p>Verifies that date setting does not modify originals through symlinks or
 * hardlinks, uses release/airdate instead of current time, and skips when the
 * date is unknown.
 */
public class ApplyDateTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void symlinkActionDoesNotModifyOriginal() throws Exception {
		File original = folder.newFile("original.mkv");
		long originalMtime = original.lastModified();
		Thread.sleep(50);

		File link = folder.newFile("link.mkv"); // placeholder
		link.delete();
		Files.createSymbolicLink(link.toPath(), original.toPath());

		Map<File, Long> dateMap = new HashMap<>();
		dateMap.put(link, System.currentTimeMillis() + 86400000L);

		new PostProcessing("date").apply(StandardRenameAction.SYMLINK, List.of(original), List.of(original), List.of(link), dateMap);

		// original mtime must not change through the symlink
		assertEquals("symlink must not modify original mtime", originalMtime, original.lastModified());
	}

	@Test
	public void hardlinkActionDoesNotModifyOriginal() throws Exception {
		File original = folder.newFile("original2.mkv");
		long originalMtime = original.lastModified();
		Thread.sleep(50);

		File link = folder.newFile("link2.mkv");
		link.delete();
		Files.createLink(link.toPath(), original.toPath());

		Map<File, Long> dateMap = new HashMap<>();
		dateMap.put(link, System.currentTimeMillis() + 86400000L);

		new PostProcessing("date").apply(StandardRenameAction.HARDLINK, List.of(original), List.of(original), List.of(link), dateMap);

		assertEquals("hardlink must not modify original mtime", originalMtime, original.lastModified());
	}

	@Test
	public void moveActionSetsReleaseDate() throws Exception {
		File dest = folder.newFile("movie.mkv");
		long releaseTime = System.currentTimeMillis() - 365L * 86400000L;

		Map<File, Long> dateMap = new HashMap<>();
		dateMap.put(dest, releaseTime);

		new PostProcessing("date").apply(StandardRenameAction.MOVE, List.of(), List.of(), List.of(dest), dateMap);

		assertEquals("mtime must be set to release date", releaseTime / 1000 * 1000, dest.lastModified() / 1000 * 1000);
	}

	@Test
	public void unknownDateSkipsModification() throws Exception {
		File dest = folder.newFile("unknown.mkv");
		long originalMtime = dest.lastModified();
		Thread.sleep(50);

		new PostProcessing("date").apply(StandardRenameAction.MOVE, List.of(), List.of(), List.of(dest), null);

		assertEquals("must not change mtime when date is unknown", originalMtime, dest.lastModified());
	}

	@Test
	public void emptyDateMapSkipsModification() throws Exception {
		File dest = folder.newFile("empty.mkv");
		long originalMtime = dest.lastModified();
		Thread.sleep(50);

		new PostProcessing("date").apply(StandardRenameAction.MOVE, List.of(), List.of(), List.of(dest), new HashMap<>());

		assertEquals("must not change mtime when date map is empty", originalMtime, dest.lastModified());
	}

	@Test
	public void testActionNeverModifies() throws Exception {
		File dest = folder.newFile("test.mkv");
		long originalMtime = dest.lastModified();
		Thread.sleep(50);

		Map<File, Long> dateMap = new HashMap<>();
		dateMap.put(dest, System.currentTimeMillis());

		new PostProcessing("date").apply(StandardRenameAction.TEST, List.of(), List.of(), List.of(dest), dateMap);

		assertEquals("test action must never modify mtime", originalMtime, dest.lastModified());
	}

}
