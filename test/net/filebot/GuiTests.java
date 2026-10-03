package net.filebot;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.filebot.ui.PanelThemeSwitchTest;

/**
 * Tests that create real windows and panels. Run by {@code ant test-gui} and require a display (use {@code xvfb-run ant test-gui} on servers and CI).
 */
@RunWith(Suite.class)
@SuiteClasses({ PanelThemeSwitchTest.class })
public class GuiTests {

}
