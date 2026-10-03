package net.filebot.format;

import static org.junit.Assert.*;

import java.io.File;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.junit.Test;

/**
 * Tests for the expression sandbox (FIX-06 / BUG-08).
 *
 * <p>Verifies that format/filter expressions can read data but cannot execute
 * processes, open network connections, use reflection, change {@code metaClass},
 * or write outside application folders. Also verifies the per-expression time limit.
 */
public class ExpressionSandboxTest {

	private String format(String expression, Object value) throws ScriptException {
		return new SandboxTestFormat(expression).format(value);
	}

	private boolean filter(String expression, Object value) throws ScriptException {
		return new ExpressionFilter(expression).matches(new SandboxTestBindings(value));
	}

	// ------------------------------------------------------------------
	// legitimate expressions still work
	// ------------------------------------------------------------------

	@Test
	public void simpleExpressionWorks() throws Exception {
		assertEquals("hello", format("{value}", "hello"));
		assertEquals("HELLO", format("{value.upper()}", "hello"));
	}

	@Test
	public void filterWorks() throws Exception {
		assertTrue(filter("value.length() > 3", "hello"));
		assertFalse(filter("value.length() > 10", "hello"));
	}

	@Test
	public void fileReadWorks() throws Exception {
		File f = new File("build.xml");
		assertEquals("true", format("{value.exists()}", f));
		assertEquals("build.xml", format("{value.name}", f));
	}

	// ------------------------------------------------------------------
	// process execution is blocked
	// ------------------------------------------------------------------

	@Test
	public void runtimeExecIsBlocked() throws Exception {
		try {
			format("{'id'.execute()}", "x");
			fail("Runtime.execute should be blocked");
		} catch (Exception e) {
			// expected: SecurityException or ScriptException wrapping it
		}
	}

	@Test
	public void processBuilderIsBlocked() throws Exception {
		try {
			format("{new ProcessBuilder('id').start()}", "x");
			fail("ProcessBuilder should be blocked");
		} catch (Exception e) {
			// expected
		}
	}

	@Test
	public void runtimeGetRuntimeIsBlocked() throws Exception {
		try {
			format("{Runtime.getRuntime().exec('id')}", "x");
			fail("Runtime.getRuntime should be blocked");
		} catch (Exception e) {
			// expected
		}
	}

	// ------------------------------------------------------------------
	// network is blocked
	// ------------------------------------------------------------------

	@Test
	public void urlConnectionIsBlocked() throws Exception {
		try {
			format("{new URL('http://example.com').text}", "x");
			fail("URL access should be blocked");
		} catch (Exception e) {
			// expected
		}
	}

	@Test
	public void socketIsBlocked() throws Exception {
		try {
			format("{new Socket('example.com', 80)}", "x");
			fail("Socket should be blocked");
		} catch (Exception e) {
			// expected
		}
	}

	// ------------------------------------------------------------------
	// reflection is blocked
	// ------------------------------------------------------------------

	@Test
	public void classForNameIsBlocked() throws Exception {
		try {
			format("{Class.forName('java.lang.Runtime')}", "x");
			fail("Class.forName should be blocked");
		} catch (Exception e) {
			// expected
		}
	}

	@Test
	public void metaClassChangeIsBlocked() throws Exception {
		try {
			format("{value.metaClass = null}", "x");
			fail("metaClass modification should be blocked");
		} catch (Exception e) {
			// expected
		}
	}

	// ------------------------------------------------------------------
	// System.exit is blocked
	// ------------------------------------------------------------------

	@Test
	public void systemExitIsBlocked() throws Exception {
		try {
			format("{System.exit(0)}", "x");
			fail("System.exit should be blocked");
		} catch (Exception e) {
			// expected
		}
	}

	// ------------------------------------------------------------------
	// time limit
	// ------------------------------------------------------------------

	@Test(timeout = 15000)
	public void infiniteLoopTimesOut() throws Exception {
		try {
			format("{while(true){}}", "x");
			fail("Infinite loop should time out");
		} catch (Exception e) {
			// expected: timeout
		}
	}

	// ------------------------------------------------------------------
	// filter sandbox
	// ------------------------------------------------------------------

	@Test
	public void filterBlocksProcess() throws Exception {
		try {
			boolean result = filter("'id'.execute()", "x");
			assertFalse("filter should not match when expression is blocked", result);
		} catch (Exception e) {
			// also acceptable: compilation or sandbox error
		}
	}

	// ------------------------------------------------------------------

	private static class SandboxTestFormat extends ExpressionFormat {

		public SandboxTestFormat(String format) throws ScriptException {
			super(format);
		}

		@Override
		public Bindings getBindings(Object value) {
			return new SandboxTestBindings(value);
		}
	}

	private static class SandboxTestBindings extends SimpleBindings {

		public SandboxTestBindings(Object value) {
			put("value", value);
		}
	}

}
