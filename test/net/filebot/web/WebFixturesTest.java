package net.filebot.web;

import static net.filebot.util.JsonUtilities.*;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.Test;

/**
 * Fixture-based parsing tests for external service responses (FIX-19).
 *
 * <p>Validates that JSON parsing works correctly on recorded API responses
 * without network access. Live contract tests remain in {@code ant test-online}.
 */
public class WebFixturesTest {

	private static String readFixture(String path) throws Exception {
		try (InputStream in = WebFixturesTest.class.getResourceAsStream("/resources/web/" + path)) {
			assertNotNull("fixture not found: " + path, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	// --- TMDb ---

	@Test
	public void tmdbSearchParsesResults() throws Exception {
		Object response = readJson(readFixture("tmdb/search-serenity.json"));
		assertNotNull(response);

		Object[] results = getArray(response, "results");
		assertNotNull(results);
		assertEquals(2, results.length);

		// first result: Serenity (Chinese title)
		String title = getString(results[0], "title");
		assertEquals("冲出宁静号", title);

		Integer id = getInteger(results[0], "id");
		assertEquals(16320, id.intValue());

		String releaseDate = getString(results[0], "release_date");
		assertEquals("2005-09-29", releaseDate);
	}

	@Test
	public void tmdbSearchParsesMovieModel() throws Exception {
		Object response = readJson(readFixture("tmdb/search-serenity.json"));
		Object[] results = getArray(response, "results");

		// build Movie from parsed data (same as TMDbClient does)
		String name = getString(results[0], "title");
		String originalTitle = getString(results[0], "original_title");
		Integer year = Integer.parseInt(getString(results[0], "release_date").substring(0, 4));
		Integer tmdbId = getInteger(results[0], "id");

		assertEquals("冲出宁静号", name);
		assertEquals("Serenity", originalTitle);
		assertEquals(2005, year.intValue());
		assertEquals(16320, tmdbId.intValue());
	}

	// --- TVMaze ---

	@Test
	public void tvmazeSearchParsesResults() throws Exception {
		Object response = readJson(readFixture("tvmaze/search-buffy.json"));
		assertNotNull(response);

		Map<?, ?>[] shows = asMapArray(response);
		assertNotNull(shows);
		assertEquals(2, shows.length);

		// first result: Buffy
		Map<?, ?> show = getMap(shows[0], "show");
		assertNotNull(show);

		assertEquals("Buffy the Vampire Slayer", getString(show, "name"));
		assertEquals(427, getInteger(show, "id").intValue());
		assertEquals("1997-03-10", getString(show, "premiered"));
		assertEquals("English", getString(show, "language"));
	}

	@Test
	public void tvmazeSearchParsesGenres() throws Exception {
		Object response = readJson(readFixture("tvmaze/search-buffy.json"));
		Map<?, ?>[] shows = asMapArray(response);
		Map<?, ?> show = getMap(shows[0], "show");

		Object[] genres = getArray(show, "genres");
		assertNotNull(genres);
		assertEquals(3, genres.length);
		assertEquals("Drama", genres[0]);
		assertEquals("Fantasy", genres[1]);
		assertEquals("Horror", genres[2]);
	}

	// --- OMDb ---

	@Test
	public void omdbMovieParsesFields() throws Exception {
		Object response = readJson(readFixture("omdb/movie-shawshank.json"));
		assertNotNull(response);

		assertEquals("The Shawshank Redemption", getString(response, "title"));
		assertEquals("1994", getString(response, "year"));
		assertEquals("tt0111161", getString(response, "imdbID"));
		assertEquals("Drama", getString(response, "genre"));
		assertEquals("Frank Darabont", getString(response, "director"));
	}

	@Test
	public void omdbMovieParsesRatings() throws Exception {
		Object response = readJson(readFixture("omdb/movie-shawshank.json"));

		Object[] ratings = getArray(response, "ratings");
		assertNotNull(ratings);
		assertEquals(3, ratings.length);

		assertEquals("Internet Movie Database", getString(ratings[0], "Source"));
		assertEquals("9.3/10", getString(ratings[0], "Value"));
	}

	@Test
	public void omdbMovieImdbRating() throws Exception {
		Object response = readJson(readFixture("omdb/movie-shawshank.json"));
		assertEquals("9.3", getString(response, "imdbRating"));
	}

	// --- AcoustID ---

	@Test
	public void acoustIdLookupParsesResults() throws Exception {
		Object response = readJson(readFixture("acoustid/lookup-yesterday.json"));
		assertNotNull(response);

		assertEquals("ok", getString(response, "status"));

		Object[] results = getArray(response, "results");
		assertNotNull(results);
		assertEquals(1, results.length);

		assertEquals("e4f53f4d-0828-d3b4-a0e8-d86b1a0e5c0c", getString(results[0], "id"));
		assertEquals(0.92, getDecimal(results[0], "score"), 0.01);
	}

	@Test
	public void acoustIdLookupParsesRecording() throws Exception {
		Object response = readJson(readFixture("acoustid/lookup-yesterday.json"));
		Object[] results = getArray(response, "results");

		Object[] recordings = getArray(results[0], "recordings");
		assertNotNull(recordings);
		assertEquals(1, recordings.length);

		assertEquals("Yesterday", getString(recordings[0], "title"));
		assertEquals(165, getInteger(recordings[0], "duration").intValue());
	}

	// --- AniDB ---

	@Test
	public void anidbTitlesFormatParsesEntries() throws Exception {
		String fixture = readFixture("anidb/anime-titles.dat");
		assertNotNull(fixture);
		assertTrue(fixture.length() > 0);

		// format: aid|type|language|title (skip comments)
		String[] lines = fixture.split("\n");
		long entries = java.util.Arrays.stream(lines)
			.filter(l -> !l.startsWith("#") && l.contains("|"))
			.count();

		assertTrue("should have parsed entries", entries >= 10);
	}

	@Test
	public void anidbTitlesFormatMatchesPattern() throws Exception {
		String fixture = readFixture("anidb/anime-titles.dat");
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(?!#)(\\d+)[|](\\d)[|]([\\w-]+)[|](.+)$");

		boolean foundCowboyBebop = fixture.lines().anyMatch(l -> {
			if (l.startsWith("#")) return false;
			java.util.regex.Matcher m = pattern.matcher(l);
			return m.matches() && m.group(4).equals("Cowboy Bebop") && m.group(3).equals("en");
		});

		assertTrue("should find Cowboy Bebop in English", foundCowboyBebop);
	}

}
