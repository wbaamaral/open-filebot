package net.filebot;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import net.filebot.History.Element;
import net.filebot.History.Sequence;

public class HistoryTest {

	@Test
	public void testExportAndImportHistory() {
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
	public void testImportLegacyOrEmptyStream() {
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

}
