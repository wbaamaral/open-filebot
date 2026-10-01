package net.filebot;

import static net.filebot.Logging.*;
import static net.filebot.Settings.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.logging.Level;

public enum ApplicationFolder {

	// real user home (the user.home will point to the application-specific container in sandbox environments)
	UserHome(isMacSandbox() ? System.getProperty("UserHome") : System.getProperty("user.home")),

	AppData(System.getProperty("application.dir", UserHome.resolve(".filebot").getPath())),

	TemporaryFiles(System.getProperty("java.io.tmpdir")),

	Cache(System.getProperty("application.cache", AppData.resolve("cache").getPath()));

	private File path;

	ApplicationFolder(String path) {
		File f = new File(path).getAbsoluteFile();
		if (f.exists()) {
			try {
				f = f.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toFile();
			} catch (IOException e) {
				debug.log(Level.FINE, e, e::toString);
			}
		}
		this.path = f;
	}

	public File get() {
		return path;
	}

	public File resolve(String name) {
		return new File(path, name);
	}

}
