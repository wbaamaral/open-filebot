package net.filebot.format;

import static net.filebot.format.ExpressionFormat.*;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

public class ExpressionFilter {

	private final String expression;
	private final CompiledScript compiledExpression;

	private Throwable lastException;

	public ExpressionFilter(String expression) throws ScriptException {
		this.expression = expression;
		this.compiledExpression = new SandboxedCompiledScript(compileScriptlet(expression));
	}

	public String getExpression() {
		return expression;
	}

	public Throwable getLastException() {
		return lastException;
	}

	public boolean matches(Object value) {
		return matches(new ExpressionBindings(value));
	}

	public boolean matches(Bindings bindings) {
		this.lastException = null;

		// initialize script context with the bindings
		ScriptContext context = new SimpleScriptContext();
		context.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);

		try {
			// evaluate user script under the sandbox
			Object value = compiledExpression.eval(context);

			// value as boolean
			return DefaultTypeTransformation.castToBoolean(value);
		} catch (Throwable e) {
			// ignore any and all scripting exceptions
			this.lastException = e;
		}

		return false;
	}

}
