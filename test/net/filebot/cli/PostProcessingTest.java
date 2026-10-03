package net.filebot.cli;

import static java.util.Collections.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.filebot.StandardRenameAction;

public class PostProcessingTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File root;
	File folder;
	File source;
	List<File> destination;

	@Before
	public void setUp() throws Exception {
		root = tmp.newFolder("input");
		folder = new File(root, "Movie.2009");
		folder.mkdirs();

		// original path of a file that has been moved away
		source = new File(folder, "Movie.2009.mkv");
		destination = singletonList(new File(tmp.getRoot(), "Movie (2009).mkv"));
	}

	void apply(String options, StandardRenameAction action) {
		new PostProcessing(options).apply(action, singleton(root), singleton(source), destination);
	}

	@Test
	public void pruneAfterMove() {
		apply("prune", StandardRenameAction.MOVE);

		assertFalse(folder.exists());
		assertTrue(root.isDirectory());
	}

	@Test
	public void testModeNeverModifiesFileSystem() {
		File keep = new File(root, "keep_empty_dir");
		keep.mkdirs();

		apply("prune,date", StandardRenameAction.TEST);

		assertTrue(folder.isDirectory());
		assertTrue(keep.isDirectory());
	}

	@Test
	public void pruneSkippedForActionsThatKeepTheSource() {
		for (StandardRenameAction action : new StandardRenameAction[] { StandardRenameAction.COPY, StandardRenameAction.SYMLINK, StandardRenameAction.HARDLINK, StandardRenameAction.CLONE, StandardRenameAction.DUPLICATE }) {
			apply("prune", action);
			assertTrue(action.name(), folder.isDirectory());
		}
	}

	@Test
	public void unknownOptionsAreIgnored() {
		apply("foo", StandardRenameAction.MOVE);

		assertTrue(folder.isDirectory());
	}

	@Test
	public void optionSeparators() {
		apply(" Date ; PRUNE ", StandardRenameAction.MOVE);

		assertFalse(folder.exists());
	}

}
