package net.filebot.archive;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.filebot.archive.Archive.Extractor;
import net.filebot.cli.CmdlineOperations;
import net.filebot.cli.ConflictAction;

/**
 * Archives with malicious entry paths (Zip Slip) must never write files outside of the output folder, with any extractor.
 */
@RunWith(Parameterized.class)
public class ArchiveExtractionTest {

	@Parameters(name = "{0}")
	public static Collection<Object[]> extractors() {
		return Arrays.asList(new Object[][] { { Extractor.SevenZipNativeBindings }, { Extractor.SevenZipExecutable }, { Extractor.ApacheVFS } });
	}

	static final String[] ILLEGAL_ENTRIES = { "../ESCAPED_1.txt", "../../ESCAPED_2.txt", "sub/../../ESCAPED_3.txt", "/tmp/ESCAPED_4.txt", "C:/ESCAPED_5.txt" };

	static final File ABSOLUTE_ENTRY = new File("/tmp/ESCAPED_4.txt");

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	final Extractor extractor;

	File workspace;
	File archive;
	File output;

	String extractorProperty;

	public ArchiveExtractionTest(Extractor extractor) {
		this.extractor = extractor;
	}

	@Before
	public void setUp() throws Exception {
		// nested folders so that ../../ still points inside the temporary folder
		workspace = tmp.newFolder("a", "b", "c");
		archive = new File(workspace, "evil.zip");
		output = new File(workspace, "output");

		try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(archive))) {
			for (String name : concat("ok.txt", "Movie/en.srt", ILLEGAL_ENTRIES)) {
				zip.putNextEntry(new ZipEntry(name));
				zip.write(name.getBytes(UTF_8));
				zip.closeEntry();
			}
		}

		extractorProperty = System.getProperty("net.filebot.Archive.extractor");
		System.setProperty("net.filebot.Archive.extractor", extractor.name());

		assumeTrue(extractor + " is not available", isAvailable());
		assumeFalse(ABSOLUTE_ENTRY + " already exists", ABSOLUTE_ENTRY.exists());
	}

	@After
	public void tearDown() {
		if (extractorProperty == null) {
			System.clearProperty("net.filebot.Archive.extractor");
		} else {
			System.setProperty("net.filebot.Archive.extractor", extractorProperty);
		}
	}

	boolean isAvailable() {
		try {
			File probe = new File(tmp.getRoot(), "probe.zip");
			try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(probe))) {
				zip.putNextEntry(new ZipEntry("probe.txt"));
				zip.closeEntry();
			}
			Archive.open(probe).close();
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	static String[] concat(String a, String b, String... c) {
		String[] s = Arrays.copyOf(new String[] { a, b }, 2 + c.length);
		System.arraycopy(c, 0, s, 2, c.length);
		return s;
	}

	void assertNothingEscaped() throws Exception {
		try (java.util.stream.Stream<java.nio.file.Path> files = Files.walk(tmp.getRoot().toPath())) {
			files.filter(p -> p.getFileName().toString().startsWith("ESCAPED")).forEach(p -> fail("Escaped: " + p));
		}
		assertFalse(ABSOLUTE_ENTRY.exists());
	}

	void extract(boolean all) throws Exception {
		// extractors may reject malicious archives as long as nothing escapes
		try {
			Archive a = Archive.open(archive);
			try {
				if (all) {
					a.extract(output);
				} else {
					a.extract(output, f -> true);
				}
			} finally {
				a.close();
			}
		} catch (Exception e) {
			assertRejectionAllowed(e);
		}
	}

	void assertRejectionAllowed(Exception e) {
		// Apache VFS refuses to open archives with illegal entry paths, all other extractors must skip illegal entries and extract everything else
		if (extractor != Extractor.ApacheVFS) {
			throw new AssertionError(extractor + " must skip illegal entries: " + e, e);
		}
	}

	@Test
	public void extractAll() throws Exception {
		extract(true);
		assertNothingEscaped();
	}

	@Test
	public void extractSelection() throws Exception {
		extract(false);
		assertNothingEscaped();
	}

	@Test
	public void extractWithCmdlineOperations() throws Exception {
		List<File> extracted;
		try {
			extracted = new CmdlineOperations().extract(Arrays.asList(archive), output, ConflictAction.SKIP, null, true);
		} catch (Exception e) {
			assertRejectionAllowed(e);
			assertNothingEscaped();
			return;
		}

		assertNothingEscaped();
		for (File f : extracted) {
			assertTrue(f.toString(), f.toPath().normalize().startsWith(output.toPath()));
		}
		assertTrue(extracted.contains(new File(output, "ok.txt")));
		assertTrue(new File(output, "ok.txt").isFile());
	}

}
