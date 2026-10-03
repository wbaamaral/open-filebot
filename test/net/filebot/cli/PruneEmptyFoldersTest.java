package net.filebot.cli;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PruneEmptyFoldersTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File root;

	@Before
	public void setUp() throws Exception {
		root = tmp.newFolder("input");
	}

	File folder(String path) {
		File f = new File(root, path);
		f.mkdirs();
		return f;
	}

	File file(String path) throws Exception {
		File f = new File(root, path);
		f.getParentFile().mkdirs();
		Files.write(f.toPath(), "x".getBytes());
		return f;
	}

	File moved(String path) {
		// original path of a file that has been moved away
		File f = new File(root, path);
		f.getParentFile().mkdirs();
		return f;
	}

	List<File> prune(File... movedFiles) {
		return new PruneEmptyFolders(singleton(root)).prune(asList(movedFiles));
	}

	@Test
	public void unrelatedEmptyFoldersArePreserved() throws Exception {
		File keep = folder("keep_empty_dir");
		File deeper = folder("sub/deeper");

		prune(moved("Avatar.2009.1080p.mkv"));

		assertTrue(keep.isDirectory());
		assertTrue(deeper.isDirectory());
		assertTrue(root.isDirectory());
	}

	@Test
	public void walkUpToInputFolder() throws Exception {
		File episode = moved("Show/Season 1/Show.S01E01.mkv");

		List<File> deleted = prune(episode);

		assertEquals(asList(new File(root, "Show/Season 1"), new File(root, "Show")), deleted);
		assertFalse(new File(root, "Show").exists());
		assertTrue(root.isDirectory());
	}

	@Test
	public void inputFolderIsNeverDeleted() throws Exception {
		prune(moved("Movie.2009.mkv"));

		assertTrue(root.isDirectory());
	}

	@Test
	public void stopAtFolderThatIsNotEmpty() throws Exception {
		File other = file("Show/Notes.txt");

		prune(moved("Show/Season 1/Show.S01E01.mkv"));

		assertFalse(new File(root, "Show/Season 1").exists());
		assertTrue(other.exists());
	}

	@Test
	public void emptySiblingFoldersAreNotSearched() throws Exception {
		File sibling = folder("Show/Season 2");

		prune(moved("Show/Season 1/Show.S01E01.mkv"));

		assertFalse(new File(root, "Show/Season 1").exists());
		assertTrue(sibling.isDirectory());
	}

	@Test
	public void thumbnailStoresCountAsEmpty() throws Exception {
		file("Movie/Thumbs.db");
		file("Movie/.DS_Store");

		prune(moved("Movie/Movie.2009.mkv"));

		assertFalse(new File(root, "Movie").exists());
	}

	@Test
	public void filesThatStillExistAreIgnored() throws Exception {
		File copied = file("Movie/Movie.2009.mkv");

		assertEquals(emptyList(), prune(copied));
		assertTrue(copied.exists());
	}

	@Test
	public void foldersOutsideInputFoldersArePreserved() throws Exception {
		File outside = tmp.newFolder("outside");
		File movedOutside = new File(outside, "Movie.2009.mkv");

		assertEquals(emptyList(), prune(movedOutside));
		assertTrue(outside.isDirectory());
	}

	@Test
	public void inputFileDoesNotAllowPruning() throws Exception {
		File movie = moved("Movie/Movie.2009.mkv");

		List<File> deleted = new PruneEmptyFolders(singleton(movie)).prune(singleton(movie));

		assertEquals(emptyList(), deleted);
		assertTrue(new File(root, "Movie").isDirectory());
	}

	@Test
	public void symlinksAreNeverFollowed() throws Exception {
		File target = tmp.newFolder("outside", "empty");
		Files.createSymbolicLink(new File(folder("Movie"), "link").toPath(), target.toPath());

		prune(moved("Movie/Movie.2009.mkv"));

		assertTrue(new File(root, "Movie").isDirectory());
		assertTrue(target.isDirectory());
	}

	@Test
	public void symlinkLoopDoesNotOverflow() throws Exception {
		File movie = folder("Movie");
		Files.createSymbolicLink(new File(movie, "loop").toPath(), movie.toPath());

		assertEquals(emptyList(), prune(moved("Movie/Movie.2009.mkv")));
		assertTrue(movie.isDirectory());
	}

	@Test
	public void symlinkedFolderIsNotDeleted() throws Exception {
		File target = tmp.newFolder("outside", "Movie");
		Files.createSymbolicLink(new File(root, "Movie").toPath(), target.toPath());

		assertEquals(emptyList(), prune(new File(root, "Movie/Movie.2009.mkv")));
		assertTrue(target.isDirectory());
	}

}
