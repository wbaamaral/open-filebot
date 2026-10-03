package net.filebot.cli;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for {@link ScriptSource} inline Groovy detection (FIX-07 / BUG-11).
 *
 * <p>Only the explicit {@code g:} prefix is accepted as inline Groovy.
 * Paths with spaces, URLs with spaces or code-like strings must not be
 * silently executed as Groovy code.
 */
public class ScriptSourceTest {

	@Test
	public void explicitPrefixIsAccepted() throws Exception {
		assertEquals("println 1", ScriptSource.INLINE_GROOVY.accept("g:println 1"));
		assertEquals("def x = 1", ScriptSource.INLINE_GROOVY.accept("g:def x = 1"));
	}

	@Test
	public void pathWithSpacesIsNotInlineGroovy() throws Exception {
		assertNull(ScriptSource.INLINE_GROOVY.accept("/media/My Scripts/sort.groovy"));
		assertNull(ScriptSource.INLINE_GROOVY.accept("C:/Program Files/script.groovy"));
	}

	@Test
	public void codeWithoutPrefixIsNotInlineGroovy() throws Exception {
		assertNull(ScriptSource.INLINE_GROOVY.accept("println 1"));
		assertNull(ScriptSource.INLINE_GROOVY.accept("def x = 1; println x"));
		assertNull(ScriptSource.INLINE_GROOVY.accept("while(true){}"));
	}

	@Test
	public void pathWithSpacesIsNotAcceptedByAnySource() throws Exception {
		try {
			ScriptSource.findScriptProvider("/media/My Scripts/sort.groovy");
			fail("should throw CmdlineException for unknown script source");
		} catch (CmdlineException e) {
			assertTrue(e.getMessage().contains("Bad script source"));
		}
	}

	@Test
	public void explicitPrefixFindsProvider() throws Exception {
		assertEquals(ScriptSource.INLINE_GROOVY, ScriptSource.findScriptProvider("g:println 1"));
	}

}
