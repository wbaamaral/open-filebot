package net.filebot;

import static java.nio.charset.StandardCharsets.*;
import static java.util.Collections.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.filebot.History.HistoryFormatException;

public class HistorySpoolerTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File historyFile;

	@Before
	public void setUp() throws Exception {
		historyFile = new File(tmp.getRoot(), "history.xml");
	}

	static class FailingHistorySpooler extends HistorySpooler {

		boolean fail = true;

		public FailingHistorySpooler(File persistentHistoryFile) {
			super(persistentHistoryFile);
		}

		@Override
		protected void writeHistory(History history, OutputStream out) throws IOException {
			if (fail) {
				// write partial output and then fail
				out.write("<?xml version=\"1.0\"?><history><seq".getBytes(UTF_8));
				throw new IOException("Disk full");
			}
			super.writeHistory(history, out);
		}
	}

	void rename(HistorySpooler spooler, String from, String to) {
		spooler.append(singletonMap(new File("/media/in", from), new File("/media/out", to)));
	}

	String content() throws IOException {
		return new String(Files.readAllBytes(historyFile.toPath()), UTF_8);
	}

	File[] backups() {
		return tmp.getRoot().listFiles((dir, name) -> name.startsWith("history.xml.corrupt-"));
	}

	@Test
	public void commitCreatesHistoryFile() throws Exception {
		HistorySpooler spooler = new HistorySpooler(historyFile);
		rename(spooler, "a.mkv", "A.mkv");
		spooler.commit();

		History history = new HistorySpooler(historyFile).getCompleteHistory();
		assertEquals(1, history.totalSize());
		assertEquals(new File("/media/out", "A.mkv"), history.getRenameMap().get(new File("/media/in", "a.mkv")));
		assertEquals(0, spooler.getSessionHistory().totalSize());
	}

	@Test
	public void commitAppendsToExistingHistory() throws Exception {
		Files.write(historyFile.toPath(), HistoryTest.VALID_XML.getBytes(UTF_8));

		HistorySpooler spooler = new HistorySpooler(historyFile);
		rename(spooler, "a.mkv", "A.mkv");
		spooler.commit();

		assertEquals(3, new HistorySpooler(historyFile).getCompleteHistory().totalSize());
	}

	@Test
	public void corruptedHistoryIsPreservedAsBackup() throws Exception {
		// PoC: history with 2 entries and missing </history>
		Files.write(historyFile.toPath(), HistoryTest.TRUNCATED_XML.getBytes(UTF_8));

		HistorySpooler spooler = new HistorySpooler(historyFile);
		rename(spooler, "a.mkv", "A.mkv");
		spooler.commit();

		File[] backups = backups();
		assertEquals(1, backups.length);
		assertEquals(HistoryTest.TRUNCATED_XML, new String(Files.readAllBytes(backups[0].toPath()), UTF_8));

		// new history file contains the current session only
		assertEquals(1, new HistorySpooler(historyFile).getCompleteHistory().totalSize());
	}

	@Test
	public void corruptedHistoryIsNotModifiedOnRead() throws Exception {
		Files.write(historyFile.toPath(), HistoryTest.TRUNCATED_XML.getBytes(UTF_8));

		try {
			new HistorySpooler(historyFile).getCompleteHistory();
			fail("Expected HistoryFormatException");
		} catch (HistoryFormatException e) {
			assertEquals(HistoryTest.TRUNCATED_XML, content());
			assertEquals(0, backups().length);
		}
	}

	@Test
	public void writeFailureKeepsPreviousHistory() throws Exception {
		Files.write(historyFile.toPath(), HistoryTest.VALID_XML.getBytes(UTF_8));

		FailingHistorySpooler spooler = new FailingHistorySpooler(historyFile);
		rename(spooler, "a.mkv", "A.mkv");
		spooler.commit();

		// previous history is untouched, no temporary files are left behind, and the session is kept
		assertEquals(HistoryTest.VALID_XML, content());
		assertEquals(Arrays.asList("history.xml", "history.xml.lock"), Arrays.asList(tmp.getRoot().list()).stream().sorted().collect(java.util.stream.Collectors.toList()));
		assertEquals(1, spooler.getSessionHistory().totalSize());

		// commit again once the problem is gone
		spooler.fail = false;
		spooler.commit();

		assertEquals(3, new HistorySpooler(historyFile).getCompleteHistory().totalSize());
		assertEquals(0, spooler.getSessionHistory().totalSize());
	}

	@Test
	public void persistentHistoryDisabled() throws Exception {
		HistorySpooler spooler = new HistorySpooler(historyFile);
		spooler.setPersistentHistoryEnabled(false);
		rename(spooler, "a.mkv", "A.mkv");
		spooler.commit();

		assertFalse(historyFile.exists());
	}

}
