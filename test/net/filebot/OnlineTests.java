package net.filebot;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.filebot.mediainfo.MediaInfoTest;
import net.filebot.web.WebTestSuite;

/**
 * Contract tests against live external services and remote sample data. Run by {@code ant test-online} and never blocking for the build, since results depend on third-party availability and data.
 */
@RunWith(Suite.class)
@SuiteClasses({ WebTestSuite.class, MediaInfoTest.class })
public class OnlineTests {

}
