package net.filebot.format;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * A {@link CompiledScript} that evaluates user expressions under the {@link ExpressionSandbox}.
 *
 * <p>Replaces {@code SecureCompiledScript} and the removed {@code SecurityManager} sandbox.
 * Compilation is rewritten by {@code SandboxTransformer} so every call/constructor/attribute
 * access goes through the sandbox interceptor at runtime. A per-expression time limit
 * (via Groovy {@code @TimedInterrupt}) guards against infinite loops.
 */
public class SandboxedCompiledScript extends CompiledScript {

	/** Default per-expression time limit in milliseconds. */
	public static final long DEFAULT_TIMEOUT_MS = 5000;

	private final CompiledScript compiledScript;
	private final long timeoutMs;

	public SandboxedCompiledScript(CompiledScript compiledScript) {
		this(compiledScript, DEFAULT_TIMEOUT_MS);
	}

	public SandboxedCompiledScript(CompiledScript compiledScript, long timeoutMs) {
		this.compiledScript = compiledScript;
		this.timeoutMs = timeoutMs;
	}

	@Override
	public Object eval(ScriptContext context) throws ScriptException {
		AtomicReference<Object> result = new AtomicReference<Object>();
		AtomicReference<Throwable> error = new AtomicReference<Throwable>();

		Thread worker = new Thread(() -> {
			try {
				Object value = ExpressionSandbox.call(() -> {
					Object v = compiledScript.eval(context);
					if (v instanceof Callable<?>) {
						return ((Callable<?>) v).call();
					}
					return v;
				});
				result.set(value);
			} catch (Throwable t) {
				error.set(t);
			}
		}, "expression-sandbox");
		worker.setDaemon(true);
		worker.start();

		try {
			worker.join(timeoutMs);
		} catch (InterruptedException e) {
			worker.interrupt();
			Thread.currentThread().interrupt();
			throw new ScriptException("Expression evaluation interrupted");
		}

		if (worker.isAlive()) {
			worker.interrupt();
			throw new ScriptException("Expression evaluation timed out after " + timeoutMs + " ms");
		}

		if (error.get() != null) {
			Throwable t = error.get();
			if (t instanceof ScriptException) {
				throw (ScriptException) t;
			}
			if (t instanceof RuntimeException) {
				throw (RuntimeException) t;
			}
			if (t instanceof Exception) {
				throw new ScriptException((Exception) t);
			}
			throw new ScriptException(String.valueOf(t));
		}

		return result.get();
	}

	@Override
	public ScriptEngine getEngine() {
		return compiledScript.getEngine();
	}
}
