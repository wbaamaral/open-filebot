package net.filebot.web;

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;

/**
 * Tests for HTTPS endpoint migration (FIX-08 / BUG-13).
 *
 * <p>Verifies that AniDB, AcoustID, OMDb and TVMaze use {@code https://}
 * for their API endpoints and public links.
 */
public class HttpsEndpointsTest {

	@Test
	public void anidbEpisodeListLinkUsesHttps() {
		AnidbClient client = new AnidbClient("filebot", 6);
		URI link = client.getEpisodeListLink(new SearchResult(4521, "Cowboy Bebop", java.util.List.of()));
		assertEquals("https", link.getScheme());
		assertEquals("anidb.net", link.getHost());
	}

	@Test
	public void anidbSourceUsesHttpsForApi() throws Exception {
		String source = readSource("web/AnidbClient.java");
		assertFalse("AniDB API must not use http://", source.contains("new URL(\"http://api.anidb.net"));
		assertFalse("AniDB index must not use http://", source.contains("new URL(\"http://anidb.net"));
		assertTrue("AniDB API must use https://", source.contains("new URL(\"https://api.anidb.net"));
		assertTrue("AniDB index must use https://", source.contains("new URL(\"https://anidb.net"));
	}

	@Test
	public void acoustidUsesHttps() throws Exception {
		String source = readSource("web/AcoustIDClient.java");
		assertFalse("AcoustID must not use http://", source.contains("new URL(\"http://api.acoustid.org"));
		assertTrue("AcoustID must use https://", source.contains("new URL(\"https://api.acoustid.org"));
	}

	@Test
	public void omdbUsesHttps() throws Exception {
		String source = readSource("web/OMDbClient.java");
		assertFalse("OMDb must not use http://", source.contains("new URL(\"http://www.omdbapi.com"));
		assertTrue("OMDb must use https://", source.contains("new URL(\"https://www.omdbapi.com"));
	}

	@Test
	public void tvmazeUsesHttps() throws Exception {
		String source = readSource("web/TVMazeClient.java");
		assertFalse("TVMaze API must not use http://", source.contains("new URL(\"http://api.tvmaze.com"));
		assertFalse("TVMaze link must not use http://", source.contains("URI.create(\"http://www.tvmaze.com"));
		assertTrue("TVMaze API must use https://", source.contains("new URL(\"https://api.tvmaze.com"));
		assertTrue("TVMaze link must use https://", source.contains("URI.create(\"https://www.tvmaze.com"));
	}

	@Test
	public void localAnidbIndexFallbackExists() throws Exception {
		java.io.File local = new java.io.File("downloads/data/anidb.txt.xz");
		assertTrue("local anidb.txt.xz fallback must exist", local.isFile());
	}

	private String readSource(String relative) throws Exception {
		return new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("source/net/filebot", relative)));
	}

}
