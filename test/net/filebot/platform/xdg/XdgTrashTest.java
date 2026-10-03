package net.filebot.platform.xdg;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class XdgTrashTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File trashFolder;
	XdgTrash trash;

	@Before
	public void setUp() throws Exception {
		trashFolder = new File(tmp.getRoot(), "Trash");
		trash = new XdgTrash(trashFolder.toPath());
	}

	File file(String path) throws Exception {
		File f = new File(tmp.getRoot(), path);
		f.getParentFile().mkdirs();
		Files.write(f.toPath(), path.getBytes(UTF_8));
		return f;
	}

	String info(String name) throws Exception {
		return new String(Files.readAllBytes(new File(trashFolder, "info/" + name + ".trashinfo").toPath()), UTF_8);
	}

	@Test
	public void moveFileToTrash() throws Exception {
		File f = file("media/Movie (2009).mkv");

		File trashed = trash.moveToTrash(f);

		assertFalse(f.exists());
		assertEquals(new File(trashFolder, "files/Movie (2009).mkv"), trashed);
		assertEquals("media/Movie (2009).mkv", new String(Files.readAllBytes(trashed.toPath()), UTF_8));

		String info = info("Movie (2009).mkv");
		assertTrue(info, info.startsWith("[Trash Info]\nPath=" + XdgTrash.encodePath(f.getAbsolutePath()) + "\n"));
		assertTrue(info, info.matches("(?s).*\nDeletionDate=\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\n"));
	}

	@Test
	public void uniqueNames() throws Exception {
		File a = trash.moveToTrash(file("a/video.mkv"));
		File b = trash.moveToTrash(file("b/video.mkv"));
		File c = trash.moveToTrash(file("c/video.mkv"));

		assertEquals("video.mkv", a.getName());
		assertEquals("video.mkv.2", b.getName());
		assertEquals("video.mkv.3", c.getName());
		assertTrue(info("video.mkv.2").contains("/b/video.mkv\n"));
	}

	@Test
	public void moveFolderToTrash() throws Exception {
		file("Season 1/Episode 1.mkv");
		file("Season 1/Episode 2.mkv");

		File trashed = trash.moveToTrash(new File(tmp.getRoot(), "Season 1"));

		assertFalse(new File(tmp.getRoot(), "Season 1").exists());
		assertTrue(new File(trashed, "Episode 2.mkv").isFile());
	}

	@Test(expected = java.io.IOException.class)
	public void missingFile() throws Exception {
		trash.moveToTrash(new File(tmp.getRoot(), "missing.mkv"));
	}

	@Test
	public void encodePath() {
		assertEquals("/media/S%C3%A9ries/Attack%20on%20Titan/a%23b%25c.mkv", XdgTrash.encodePath("/media/Séries/Attack on Titan/a#b%c.mkv"));
		assertEquals("/anime/%E9%80%B2%E6%92%83.mkv", XdgTrash.encodePath("/anime/進撃.mkv"));
	}

}
