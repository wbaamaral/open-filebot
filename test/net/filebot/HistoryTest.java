package net.filebot;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import net.filebot.History.Element;
import net.filebot.History.HistoryFormatException;
import net.filebot.History.Sequence;

public class HistoryTest {

	@Test
	public void testExportAndImportHistory() throws Exception {
		History history = new History();
		List<Element> elements = Arrays.asList(
			new Element("Firefly.S01E01.mkv", "Firefly - 1x01 - The Train Job.mkv", new File("/media/tv/Firefly")),
			new Element("Firefly.S01E02.mkv", "Firefly - 1x02 - Bushwhacked.mkv", new File("/media/tv/Firefly"))
		);
		history.add(elements);

		assertEquals(1, history.sequences().size());
		assertEquals(2, history.totalSize());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		History.exportHistory(history, out);

		String xml = new String(out.toByteArray(), StandardCharsets.UTF_8);
		assertTrue(xml.contains("<history>"));
		assertTrue(xml.contains("<sequence date="));
		assertTrue(xml.contains("from=\"Firefly.S01E01.mkv\""));
		assertTrue(xml.contains("to=\"Firefly - 1x01 - The Train Job.mkv\""));

		// import back
		History imported = History.importHistory(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(1, imported.sequences().size());
		assertEquals(2, imported.totalSize());

		Sequence seq = imported.sequences().get(0);
		assertNotNull(seq.date());
		assertEquals(2, seq.elements().size());

		Element elem1 = seq.elements().get(0);
		assertEquals("Firefly.S01E01.mkv", elem1.from());
		assertEquals("Firefly - 1x01 - The Train Job.mkv", elem1.to());
		assertEquals(new File("/media/tv/Firefly"), elem1.dir());

		Map<File, File> map = imported.getRenameMap();
		assertEquals(2, map.size());
		assertEquals(new File("/media/tv/Firefly/Firefly - 1x01 - The Train Job.mkv"), map.get(new File("/media/tv/Firefly/Firefly.S01E01.mkv")));
	}

	@Test
	public void testImportLegacyOrEmptyStream() throws Exception {
		History empty = History.importHistory(new ByteArrayInputStream(new byte[0]));
		assertNotNull(empty);
		assertEquals(0, empty.sequences().size());

		String legacyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<history>\n"
				+ "    <sequence date=\"2020-01-01T12:00:00Z\">\n"
				+ "        <rename dir=\"/test/dir\" from=\"old.mp4\" to=\"new.mp4\"/>\n"
				+ "    </sequence>\n"
				+ "</history>";

		History legacy = History.importHistory(new ByteArrayInputStream(legacyXml.getBytes(StandardCharsets.UTF_8)));
		assertEquals(1, legacy.sequences().size());
		assertEquals(1, legacy.totalSize());
		assertEquals("old.mp4", legacy.sequences().get(0).elements().get(0).from());
		assertEquals("new.mp4", legacy.sequences().get(0).elements().get(0).to());
	}

	static final String VALID_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<history>\n  <sequence date=\"2020-01-01T00:00:00Z\">\n    <rename dir=\"/old\" from=\"x.mkv\" to=\"/old/y.mkv\"/>\n    <rename dir=\"/old\" from=\"z.mkv\" to=\"/old/w.mkv\"/>\n  </sequence>\n</history>\n";

	// valid history document without the closing </history> tag
	static final String TRUNCATED_XML = VALID_XML.substring(0, VALID_XML.indexOf("</history>"));

	History importString(String xml) throws Exception {
		return History.importHistory(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	@Test(expected = HistoryFormatException.class)
	public void testImportTruncatedFails() throws Exception {
		importString(TRUNCATED_XML);
	}

	@Test(expected = HistoryFormatException.class)
	public void testImportWrongRootElementFails() throws Exception {
		importString("<?xml version=\"1.0\"?><settings/>");
	}

	@Test(expected = HistoryFormatException.class)
	public void testImportDoctypeFails() throws Exception {
		importString("<?xml version=\"1.0\"?><!DOCTYPE history [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><history>&x;</history>");
	}

	@Test
	public void testImportErrorIsNotPrintedToStandardError() throws Exception {
		PrintStream stderr = System.err;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			System.setErr(new PrintStream(buffer, true, "UTF-8"));
			importString(TRUNCATED_XML);
			fail("Expected HistoryFormatException");
		} catch (HistoryFormatException e) {
			assertEquals("", buffer.toString("UTF-8"));
		} finally {
			System.setErr(stderr);
		}
	}

	@Test
	public void testImportSkipsIncompleteEntries() throws Exception {
		History history = importString("<history><sequence date=\"2020-01-01T00:00:00Z\"><rename dir=\"/a\" from=\"b.mkv\"/><rename dir=\"/a\" from=\"c.mkv\" to=\"d.mkv\"/></sequence></history>");
		assertEquals(1, history.totalSize());
		assertEquals("c.mkv", history.sequences().get(0).elements().get(0).from());
	}

	@Test
	public void testRoundTripSpecialCharacters() throws Exception {
		History history = new History();
		history.add(Arrays.asList(new Element("Amélie & \"Co\" <1>.mkv", "/média/Filmes/Amélie (2001)/日本語 – ü.mkv", new File("/downloads/ação"))));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		History.exportHistory(history, out);
		History imported = History.importHistory(new ByteArrayInputStream(out.toByteArray()));

		assertEquals(history.getRenameMap(), imported.getRenameMap());
		assertEquals(history.sequences().get(0).date(), imported.sequences().get(0).date());
	}

}
