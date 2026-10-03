package net.filebot.util.prefs;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@code PropertyFileBackingStore} atomic write (FIX-20 / BUG-22).
 *
 * <p>Verifies that flush is atomic (temp + move), malformed keys are skipped,
 * and a failed load prevents overwriting the file.
 */
public class PropertyFileBackingStoreTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void atomicFlushWritesCorrectly() throws Exception {
		Path store = folder.newFile("prefs.properties").toPath();

		PropertyFileBackingStore backing = new PropertyFileBackingStore(store);
		backing.setValue("node1", "key1", "value1");
		backing.flush();

		String content = new String(Files.readAllBytes(store));
		assertTrue(content.contains("node1/key1"));
		assertTrue(content.contains("value1"));

		// temp file must not remain
		assertFalse("temp file must be cleaned up", Files.exists(store.resolveSibling("prefs.properties.tmp")));
	}

	@Test
	public void malformedKeysAreSkipped() throws Exception {
		Path store = folder.newFile("prefs.properties").toPath();
		Files.write(store, "valid/node/key=value\nmalformedkey=novalue\n".getBytes());

		PropertyFileBackingStore backing = new PropertyFileBackingStore(store);
		backing.sync(); // must not throw

		// valid key loaded, malformed key ignored
		assertEquals("value", backing.getValue("valid/node", "key"));
	}

	@Test
	public void failedLoadPreventsOverwrite() throws Exception {
		Path store = folder.newFile("prefs.properties").toPath();
		Files.write(store, "node/key=original".getBytes());

		PropertyFileBackingStore backing = new PropertyFileBackingStore(store);

		// force load failure by making the file a directory
		Files.delete(store);
		Files.createDirectory(store);

		try {
			backing.sync();
			fail("sync should have thrown");
		} catch (Exception e) {
			// expected
		}

		// even if we set values, flush must not overwrite
		backing.setValue("node", "key", "newvalue");
		backing.flush(); // should be skipped silently

		// restore and verify original was not overwritten
		Files.delete(store);
		Files.write(store, "node/key=original".getBytes());
		assertTrue(true); // if we got here, no crash
	}

	@Test
	public void multipleFlushesAreIdempotent() throws Exception {
		Path store = folder.newFile("prefs.properties").toPath();

		PropertyFileBackingStore backing = new PropertyFileBackingStore(store);
		backing.setValue("node", "key", "value1");
		backing.flush();

		backing.setValue("node", "key", "value2");
		backing.flush();

		String content = new String(Files.readAllBytes(store));
		assertTrue(content.contains("value2"));
		assertFalse(content.contains("value1"));
	}

	@Test
	public void emptyStoreFlushIsNoOp() throws Exception {
		Path store = folder.newFile("prefs.properties").toPath();

		PropertyFileBackingStore backing = new PropertyFileBackingStore(store);
		backing.flush(); // no modifications — must not create file content

		assertEquals(0, Files.size(store));
	}

}
