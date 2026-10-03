package net.filebot.cli;

import static java.util.Arrays.*;
import static net.filebot.Settings.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.junit.Test;
import org.tukaani.xz.XZInputStream;

import net.filebot.cli.ScriptBundle.Source;

/**
 * Scripts are only executed from a script bundle that matches the expected checksum.
 */
public class ScriptBundleTest {

	static byte[] bundled() throws Exception {
		try (InputStream in = ScriptBundleTest.class.getResourceAsStream("/scripts/m1.jar.xz")) {
			assertNotNull("bundled script package", in);
			return new XZInputStream(in).readAllBytes();
		}
	}

	static String expected() {
		return getApplicationProperty("script.bundle.sha256");
	}

	@Test
	public void bundledPackageMatchesConfiguredChecksum() throws Exception {
		// fails if downloads/scripts/m1.jar.xz is updated without updating script.bundle.sha256 in app.properties
		assertEquals(expected(), ScriptBundle.sha256(bundled()));
	}

	@Test
	public void trustedBundle() throws Exception {
		byte[] data = bundled();
		ScriptBundle bundle = new ScriptBundle(asList(new Source("test", () -> data)), expected());

		assertTrue(bundle.getScript("sysinfo").length() > 0);
	}

	@Test(expected = ScriptIntegrityException.class)
	public void tamperedBundleIsRejected() throws Exception {
		byte[] data = bundled();
		data[data.length / 2] ^= 1;

		new ScriptBundle(asList(new Source("tampered", () -> data)), expected()).getScript("sysinfo");
	}

	@Test
	public void fallbackToNextTrustedSource() throws Exception {
		byte[] data = bundled();
		byte[] tampered = data.clone();
		tampered[100] ^= 1;

		ScriptBundle bundle = new ScriptBundle(asList(new Source("missing", () -> null), new Source("broken", () -> {
			throw new java.io.IOException("offline");
		}), new Source("tampered", () -> tampered), new Source("trusted", () -> data)), expected());

		assertTrue(bundle.getScript("amc").length() > 1000);
	}

	@Test(expected = ScriptIntegrityException.class)
	public void missingChecksumIsRejected() throws Exception {
		byte[] data = bundled();
		new ScriptBundle(asList(new Source("test", () -> data)), "").getScript("sysinfo");
	}

	@Test
	public void sourcesAreNeverRelativeToWorkingDirectory() throws Exception {
		List<Source> sources = ScriptSource.getScriptBundleSources(ScriptSource.GITHUB_STABLE.getCache());

		for (Source source : sources) {
			boolean absolute = source.name.startsWith("classpath:") || new File(source.name).isAbsolute() || new URI(source.name).isAbsolute();
			assertTrue(source.name, absolute);
		}
	}

	@Test
	public void scriptSourceUsesTrustedBundle() throws Exception {
		// bundled package is preferred, no network access required
		assertTrue(ScriptSource.GITHUB_STABLE.getScriptProvider("sysinfo").getScript("sysinfo").length() > 0);
	}

}
