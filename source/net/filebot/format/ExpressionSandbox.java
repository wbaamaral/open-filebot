package net.filebot.format;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.groovy.sandbox.GroovyInterceptor;

import net.filebot.ApplicationFolder;

/**
 * Runtime sandbox for format/filter expressions ({@code {...}} and {@code --filter}).
 *
 * <p>Replaces the removed {@code SecurityManager} sandbox (JEP 486). Every method call,
 * constructor invocation and attribute access is intercepted and checked against a
 * whitelist derived from the old policy: read files and metadata, block processes,
 * network, reflection, {@code ClassLoader}, {@code System.exit}, {@code evaluate} and
 * {@code metaClass} changes. Writes are allowed only inside {@link ApplicationFolder} directories.
 */
public class ExpressionSandbox extends GroovyInterceptor {

	/** Method names that indicate process execution, exit or dynamic evaluation. */
	private static final Set<String> BLOCKED_METHODS = Set.of("execute", "exec", "exit", "exitValue", "destroy", "load", "loadLibrary", "evaluate", "parse", "forName", "getMethod", "getDeclaredMethod", "getMethods", "getDeclaredMethods", "getField", "getDeclaredField", "getConstructor", "getDeclaredConstructor", "setAccessible", "invoke", "setMetaClass", "invokeMethod", "newInstance", "newProcessBuilder", "start");

	/** Fully-qualified type names that must never be reachable. */
	private static final Set<String> BLOCKED_TYPES = Set.of("java.lang.Runtime", "java.lang.ProcessBuilder", "java.lang.Process", "java.lang.ProcessImpl", "java.lang.ClassLoader", "java.net.URLClassLoader", "java.net.URL", "java.net.Socket", "java.net.ServerSocket", "java.net.DatagramSocket", "java.net.URLConnection", "java.net.HttpURLConnection", "java.net.URI", "groovy.lang.GroovyShell", "groovy.lang.GroovyClassLoader", "javax.script.ScriptEngineManager", "java.lang.reflect.Method", "java.lang.reflect.Field", "java.lang.reflect.Constructor", "java.lang.reflect.Proxy");

	/** File / path method prefixes that indicate a write or delete. */
	private static final List<String> WRITE_PREFIXES = List.of("write", "create", "delete", "rename", "mkdir", "mkdirs", "setLastModified", "setReadOnly", "setWritable", "setExecutable", "transferTo", "copyTo", "moveTo", "append", "leftShift", "withOutputStream", "withWriter", "withPrintWriter", "newOutputStream", "newWriter", "newPrintWriter", "renameTo");

	// ------------------------------------------------------------------
	// entry points
	// ------------------------------------------------------------------

	public static void run(Runnable action) {
		ExpressionSandbox sandbox = new ExpressionSandbox();
		sandbox.register();
		try {
			action.run();
		} finally {
			sandbox.unregister();
		}
	}

	public static <T> T call(java.util.concurrent.Callable<T> action) throws Exception {
		ExpressionSandbox sandbox = new ExpressionSandbox();
		sandbox.register();
		try {
			return action.call();
		} finally {
			sandbox.unregister();
		}
	}

	// ------------------------------------------------------------------
	// interceptor callbacks (groovy-sandbox 1.19 API)
	// ------------------------------------------------------------------

	@Override
	public Object onMethodCall(Invoker invoker, Object receiver, String method, Object... args) throws Throwable {
		checkCall(receiver, method, args);
		return invoker.call(receiver, method, args);
	}

	@Override
	public Object onStaticCall(Invoker invoker, Class receiver, String method, Object... args) throws Throwable {
		// block static calls on Class (forName, etc.) and other blocked types
		if (receiver == Class.class || isBlockedType(receiver)) {
			throw new SecurityException("Sandbox: static call on " + (receiver != null ? receiver.getName() : "null") + " is not allowed");
		}
		checkCall(null, method, args);
		return invoker.call(receiver, method, args);
	}

	@Override
	public Object onNewInstance(Invoker invoker, Class receiver, Object... args) throws Throwable {
		checkType(receiver);
		if (receiver != null && isBlockedType(receiver)) {
			throw new SecurityException("Sandbox: construction of " + receiver.getName() + " is not allowed");
		}
		return invoker.call(receiver, "<init>", args);
	}

	@Override
	public Object onGetProperty(Invoker invoker, Object receiver, String property) throws Throwable {
		if ("metaClass".equals(property)) {
			throw new SecurityException("Sandbox: access to metaClass is not allowed");
		}
		checkType(receiver);
		return invoker.call(receiver, property);
	}

	@Override
	public Object onSetProperty(Invoker invoker, Object receiver, String property, Object value) throws Throwable {
		if ("metaClass".equals(property)) {
			throw new SecurityException("Sandbox: modification of metaClass is not allowed");
		}
		checkType(receiver);
		checkType(value);
		return invoker.call(receiver, property, value);
	}

	@Override
	public Object onGetAttribute(Invoker invoker, Object receiver, String attribute) throws Throwable {
		if ("metaClass".equals(attribute)) {
			throw new SecurityException("Sandbox: access to metaClass is not allowed");
		}
		checkType(receiver);
		return invoker.call(receiver, attribute);
	}

	@Override
	public Object onSetAttribute(Invoker invoker, Object receiver, String attribute, Object value) throws Throwable {
		if ("metaClass".equals(attribute)) {
			throw new SecurityException("Sandbox: modification of metaClass is not allowed");
		}
		checkType(receiver);
		checkType(value);
		return invoker.call(receiver, attribute, value);
	}

	@Override
	public Object onGetArray(Invoker invoker, Object receiver, Object index) throws Throwable {
		checkType(receiver);
		return invoker.call(receiver, "getAt", index);
	}

	@Override
	public Object onSetArray(Invoker invoker, Object receiver, Object index, Object value) throws Throwable {
		checkType(receiver);
		checkType(value);
		return invoker.call(receiver, "putAt", index, value);
	}

	// ------------------------------------------------------------------
	// policy checks
	// ------------------------------------------------------------------

	private void checkCall(Object receiver, String method, Object[] args) {
		if (method == null) {
			return;
		}

		String name = method.toLowerCase();

		// 1. process execution / exit / evaluate / reflection entry points
		if (BLOCKED_METHODS.contains(name)) {
			throw new SecurityException("Sandbox: method not allowed: " + method);
		}

		// 2. blocked receiver types
		checkType(receiver);
		if (args != null) {
			for (Object arg : args) {
				checkType(arg);
			}
		}

		// 3. reflective escape via Class / ClassLoader
		if (receiver instanceof Class || receiver instanceof ClassLoader) {
			if (BLOCKED_METHODS.contains(name)) {
				throw new SecurityException("Sandbox: reflection is not allowed: " + method);
			}
		}

		// 4. System.exit / Runtime
		if (receiver == System.class) {
			throw new SecurityException("Sandbox: System control is not allowed");
		}

		// 5. file writes outside application folders
		if (receiver instanceof File && WRITE_PREFIXES.stream().anyMatch(name::startsWith)) {
			checkFileWrite((File) receiver);
		}

		// 6. network
		if (name.startsWith("connect") || name.startsWith("openConnection") || name.startsWith("openStream")) {
			if (isNetworkReceiver(receiver)) {
				throw new SecurityException("Sandbox: network access is not allowed: " + method);
			}
		}
	}

	private void checkType(Object object) {
		if (object == null) {
			return;
		}
		Class<?> type = object instanceof Class ? (Class<?>) object : object.getClass();
		if (isBlockedType(type)) {
			throw new SecurityException("Sandbox: access to " + type.getName() + " is not allowed");
		}
	}

	private boolean isBlockedType(Class<?> type) {
		if (type == null) {
			return false;
		}
		String name = type.getName();
		if (BLOCKED_TYPES.contains(name)) {
			return true;
		}
		// block ClassLoader, Process, Runtime and their subclasses
		if (ClassLoader.class.isAssignableFrom(type) || Process.class.isAssignableFrom(type) || Runtime.class.isAssignableFrom(type)) {
			return true;
		}
		return false;
	}

	private boolean isNetworkReceiver(Object receiver) {
		if (receiver == null) {
			return false;
		}
		String name = receiver.getClass().getName();
		return name.startsWith("java.net.") || name.startsWith("javax.net.");
	}

	private void checkFileWrite(File file) {
		File absolute = file.getAbsoluteFile();
		for (ApplicationFolder folder : ApplicationFolder.values()) {
			try {
				File root = folder.get().getAbsoluteFile();
				if (absolute.getPath().startsWith(root.getPath() + File.separator) || absolute.equals(root)) {
					return;
				}
			} catch (Throwable ignore) {
			}
		}
		throw new SecurityException("Sandbox: write outside application folders is not allowed: " + file);
	}
}
