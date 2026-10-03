package net.filebot;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

/**
 * Tests for GitHub Actions workflow configuration (FIX-21 / BUG-23).
 *
 * <p>Verifies that the release workflow has a single trigger, runs tests
 * before packaging, and verifies checksums of downloaded tools.
 */
public class WorkflowTest {

	@Test
	public void releaseWorkflowHasSingleTrigger() throws Exception {
		String yml = read(".github/workflows/release-build.yml");

		// must not have both release: events and push: tags: (BUG-23: caused duplicate runs)
		assertFalse("must not trigger on release events", yml.contains("types: [published, created]") || yml.contains("types:\n    - published"));
		assertTrue("must trigger on tag push", yml.contains("push:") && yml.contains("tags:"));
	}

	@Test
	public void releaseWorkflowRunsTestsBeforePackaging() throws Exception {
		String yml = read(".github/workflows/release-build.yml");

		int testStep = yml.indexOf("ant test");
		int buildStep = yml.indexOf("ant fatjar");

		assertTrue("must have test step", testStep >= 0);
		assertTrue("must have build step", buildStep >= 0);
		assertTrue("tests must run before packaging", testStep < buildStep);
	}

	@Test
	public void releaseWorkflowVerifiesChecksums() throws Exception {
		String yml = read(".github/workflows/release-build.yml");

		assertTrue("must verify SHA-256 of ivy", yml.contains("sha256sum") && yml.contains("ivy"));
		assertTrue("must verify SHA-256 of xz", yml.contains("sha256sum") && yml.contains("xz"));
	}

	@Test
	public void ciWorkflowExistsForPRs() throws Exception {
		String yml = read(".github/workflows/ci.yml");

		assertTrue("CI must trigger on pull_request", yml.contains("pull_request"));
		assertTrue("CI must run tests", yml.contains("ant test"));
	}

	@Test
	public void releaseOnlyOnTags() throws Exception {
		String yml = read(".github/workflows/release-build.yml");

		// release step must only run on tags
		assertTrue("release step must check for tags", yml.contains("startsWith(github.ref, 'refs/tags/')"));
		assertFalse("must not trigger on release created", yml.contains("types: [published, created]"));
	}

	private String read(String path) throws Exception {
		return new String(Files.readAllBytes(Path.of(path)));
	}

}
