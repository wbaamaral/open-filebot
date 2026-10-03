package net.filebot.cli;

import static java.util.Arrays.*;
import static net.filebot.CachedResource.*;
import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.tukaani.xz.XZInputStream;

import net.filebot.Cache;
import net.filebot.CacheType;

public enum ScriptSource {

	GITHUB_STABLE {

		@Override
		public String accept(String input) {
			return input.startsWith("fn:") ? input.substring(3) : null;
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			return new ScriptBundle(getScriptBundleSources(getCache()), getApplicationProperty("script.bundle.sha256"));
		}

	},

	GITHUB_MASTER {

		@Override
		public String accept(String input) {
			return input.startsWith("dev:") ? input.substring(4) : null;
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			URI parent = new URI(getApplicationProperty("github.master"));

			// NOTE: GitHub only supports If-None-Match (If-Modified-Since is ignored)
			return n -> getCache().text(n, s -> parent.resolve(s + ".groovy").toURL()).fetch(fetchIfNoneMatch(url -> n, getCache())).expire(Cache.ONE_DAY).get();
		}

	},

	INLINE_GROOVY {

		@Override
		public String accept(String input) {
			if (input.startsWith("g:")) {
				return input.substring(2);
			}
			return null;
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			return g -> g;
		}

	},

	REMOTE_URL {

		@Override
		public String accept(String input) {
			// absolute paths on Windows appear to be valid URIs so we need explicitly exclude them (e.g. C:\path\to\script.groovy)
			if (input.length() < 2 || input.charAt(1) == ':') {
				return null;
			}

			try {
				URI uri = new URI(input);
				if (uri.isAbsolute()) {
					return getName(new File(uri.getPath()));
				}
			} catch (Exception e) {
				debug.finest(e::toString);
			}
			return null;
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			URI parent = new URI(input).resolve(".");

			return n -> getCache().text(n, s -> parent.resolve(s + ".groovy").toURL()).expire(Duration.ZERO).get();
		}

	},

	LOCAL_FILE {

		@Override
		public String accept(String input) {
			try {
				File f = new File(input).getCanonicalFile();
				if (f.isFile()) {
					return getName(f);
				}
			} catch (Exception e) {
				debug.finest(e::toString);
			}
			return null;
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			File base = new File(input).getCanonicalFile().getParentFile();

			return f -> readTextFile(new File(base, f + ".groovy"));
		}

	};

	/**
	 * Possible locations of the script bundle in order of preference. Every location is verified against the expected checksum. Paths relative to the working directory are never used.
	 */
	static List<ScriptBundle.Source> getScriptBundleSources(Cache cache) throws Exception {
		List<ScriptBundle.Source> sources = new ArrayList<ScriptBundle.Source>();

		// 1. bundled with the application
		sources.add(new ScriptBundle.Source("classpath:/scripts/m1.jar.xz", () -> {
			try (InputStream in = ScriptSource.class.getResourceAsStream("/scripts/m1.jar.xz")) {
				return in == null ? null : new XZInputStream(in).readAllBytes();
			}
		}));

		// 2. installed with the system package
		File system = new File("/opt/filebot/scripts/m1.jar.xz");
		sources.add(new ScriptBundle.Source(system.getPath(), () -> {
			if (!system.isFile()) {
				return null;
			}
			try (InputStream in = new XZInputStream(new FileInputStream(system))) {
				return in.readAllBytes();
			}
		}));

		// 3. download
		URI resource = new URI(getApplicationProperty("github.stable"));
		sources.add(new ScriptBundle.Source(resource.toString(), cache.bytes(resource, URI::toURL, XZInputStream::new).expire(Cache.ONE_WEEK)));

		return sources;
	}

	public abstract String accept(String input);

	public abstract ScriptProvider getScriptProvider(String input) throws Exception;

	public Cache getCache() {
		return Cache.getCache(name(), CacheType.Persistent);
	}

	public static ScriptSource findScriptProvider(String input) throws Exception {
		return stream(values()).filter(s -> s.accept(input) != null).findFirst().orElseThrow(() -> {
			return new CmdlineException("Bad script source: " + input);
		});
	}

}
