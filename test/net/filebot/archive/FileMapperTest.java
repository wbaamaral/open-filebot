package net.filebot.archive;

import static org.junit.Assert.*;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileMapperTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File output = new File("/media/output");

	File resolve(String entry) {
		return FileMapper.resolve(output, new File(entry));
	}

	@Test
	public void legalEntryPaths() {
		assertEquals(new File(output, "movie.mkv"), resolve("movie.mkv"));
		assertEquals(new File(output, "Movie/Subs/en.srt"), resolve("Movie/Subs/en.srt"));
		assertEquals(new File(output, "Movie/Subs/en.srt"), resolve("Movie\\Subs\\en.srt"));
		assertEquals(new File(output, "a..b.txt"), resolve("a..b.txt"));
		assertEquals(new File(output, "..hidden"), resolve("..hidden"));
		assertEquals(new File(output, "b.txt"), resolve("a/../b.txt"));
		assertEquals(new File(output, "a/b.txt"), resolve("./a/b.txt"));
	}

	@Test
	public void illegalEntryPaths() {
		for (String entry : new String[] { "../ESCAPED.txt", "../../ESCAPED.txt", "a/../../ESCAPED.txt", "..\\ESCAPED.txt", "a\\..\\..\\ESCAPED.txt", "/etc/ESCAPED", "\\\\server\\share\\ESCAPED", "C:\\ESCAPED.txt", "C:ESCAPED.txt", "c:/ESCAPED.txt", "..", ".", "", "a/.." }) {
			assertNull(entry, resolve(entry));
			assertFalse(entry, FileMapper.isSafeEntryPath(entry));
		}
	}

	@Test
	public void flattenIgnoresFolders() {
		FileMapper mapper = new FileMapper(output, true);
		assertEquals(new File(output, "en.srt"), mapper.getOutputFile(new File("Movie/Subs/en.srt")));
		assertNull(mapper.getOutputFile(new File("Movie/..")));
	}

	@Test
	public void streamForIllegalEntryIsNull() throws Exception {
		File out = tmp.newFolder("output");
		FileMapper mapper = new FileMapper(out);

		assertNull(mapper.getStream(new File("../ESCAPED.txt")));
		assertFalse(new File(tmp.getRoot(), "ESCAPED.txt").exists());
	}

	@Test
	public void streamThroughExistingSymlinkIsNull() throws Exception {
		File out = tmp.newFolder("output");
		File outside = tmp.newFolder("outside");
		Files.createSymbolicLink(new File(out, "link").toPath(), outside.toPath());

		assertNull(new FileMapper(out).getStream(new File("link/ESCAPED.txt")));
		assertFalse(new File(outside, "ESCAPED.txt").exists());
	}

	@Test
	public void streamForLegalEntry() throws Exception {
		File out = tmp.newFolder("output");

		try (OutputStream stream = new FileMapper(out).getStream(new File("Movie/en.srt"))) {
			stream.write(1);
		}

		assertEquals(1, new File(out, "Movie/en.srt").length());
	}

	@Test
	public void pathFilterIgnoresIllegalEntries() {
		FileMapper mapper = new FileMapper(output);
		assertTrue(mapper.newPathFilter(java.util.Collections.singleton(new File(output, "a.txt").getPath())).accept(new File("a.txt")));
		assertFalse(mapper.newPathFilter(java.util.Collections.singleton(new File(output, "a.txt").getPath())).accept(new File("../a.txt")));
	}

}
