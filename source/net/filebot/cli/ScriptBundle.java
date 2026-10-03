package net.filebot.cli;

import static java.nio.charset.StandardCharsets.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import net.filebot.Resource;
import net.filebot.util.ByteBufferOutputStream;

/**
 * Scripts from a jar bundle that is only used if it matches the expected SHA-256 checksum.
 */
public class ScriptBundle implements ScriptProvider {

	private final Resource<byte[]> bundle;

	/**
	 * @param sources
	 *            possible locations of the bundle in order of preference; the first one that matches the checksum is used
	 * @param sha256
	 *            expected SHA-256 checksum of the uncompressed jar
	 */
	public ScriptBundle(List<Source> sources, String sha256) {
		this.bundle = ((Resource<byte[]>) () -> select(sources, sha256)).memoize();
	}

	public static class Source {

		public final String name;
		public final Resource<byte[]> data;

		public Source(String name, Resource<byte[]> data) {
			this.name = name;
			this.data = data;
		}
	}

	private static byte[] select(List<Source> sources, String sha256) throws Exception {
		if (sha256 == null || !sha256.matches("\\p{XDigit}{64}")) {
			throw new ScriptIntegrityException("Script bundle checksum is not configured");
		}

		for (Source source : sources) {
			byte[] bytes;
			try {
				bytes = source.data.get();
			} catch (Exception e) {
				debug.fine(format("Script bundle not available: %s (%s)", source.name, e));
				continue;
			}

			if (bytes == null) {
				continue;
			}

			String checksum = sha256(bytes);
			if (checksum.equalsIgnoreCase(sha256)) {
				debug.fine(format("Use script bundle: %s", source.name));
				return bytes;
			}

			log.warning(format("Ignore script bundle with unexpected checksum: %s (%s)", source.name, checksum));
		}

		throw new ScriptIntegrityException("No trusted script bundle found. Scripts are only executed from a script bundle that matches the expected checksum.");
	}

	public static String sha256(byte[] bytes) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}

	@Override
	public String getScript(String name) throws Exception {
		try (JarInputStream jar = new JarInputStream(new ByteArrayInputStream(bundle.get()), false)) {
			for (JarEntry f = jar.getNextJarEntry(); f != null; f = jar.getNextJarEntry()) {
				if (f.isDirectory() || !f.getName().startsWith(name) || !f.getName().substring(name.length()).equals(".groovy"))
					continue;

				// completely read current jar entry
				ByteBufferOutputStream buffer = new ByteBufferOutputStream(f.getSize() > 0 ? f.getSize() : 8192);
				buffer.transferFully(jar);

				jar.closeEntry();
				return UTF_8.decode(buffer.getByteBuffer()).toString();
			}
		}

		// script does not exist
		throw new FileNotFoundException("Script not found: " + name);
	}

	public Map<String, String> getManifest() throws Exception {
		try (JarInputStream jar = new JarInputStream(new ByteArrayInputStream(bundle.get()), false)) {
			return jar.getManifest().getMainAttributes().entrySet().stream().collect(toMap(it -> it.getKey().toString(), it -> it.getValue().toString()));
		}
	}

}
