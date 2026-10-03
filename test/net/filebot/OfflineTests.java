package net.filebot;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.filebot.archive.ArchiveExtractionTest;
import net.filebot.archive.FileMapperTest;
import net.filebot.cli.PostProcessingTest;
import net.filebot.cli.ScriptBundleTest;
import net.filebot.cli.ScriptSourceTest;
import net.filebot.cli.ExecCommandTest;
import net.filebot.cli.GroovyPadCancelTest;
import net.filebot.cli.ApplyDateTest;
import net.filebot.cli.ConflictIndexTest;
import net.filebot.cli.PruneEmptyFoldersTest;
import net.filebot.format.ExpressionFormatTest;
import net.filebot.format.ExpressionSandboxTest;
import net.filebot.format.HdrRegexTest;
import net.filebot.platform.xdg.XdgTrashTest;
import net.filebot.hash.VerificationFormatTest;
import net.filebot.media.MediaDetectionTest;
import net.filebot.media.ReleaseInfoTest;
import net.filebot.media.ReleaseInfoDataTest;
import net.filebot.media.VideoFormatTest;
import net.filebot.similarity.EpisodeMetricsTest;
import net.filebot.similarity.SimilarityTestSuite;
import net.filebot.similarity.DateMetricTest;
import net.filebot.subtitle.SubtitleReaderTestSuite;
import net.filebot.ui.SupportDialogTest;
import net.filebot.ui.ThemeSwitchTest;
import net.filebot.ui.ShortcutDocumentationTest;
import net.filebot.ui.rename.MatchModelTest;
import net.filebot.util.UtilTestSuite;
import net.filebot.util.prefs.PropertyFileBackingStoreTest;
import net.filebot.util.ui.ProgressMonitorEDTTest;
import net.filebot.util.ui.ProgressMonitorCancelTest;
import net.filebot.util.ui.ThemeDetectionTest;
import net.filebot.web.SimpleDateTest;
import net.filebot.web.HttpsEndpointsTest;
import net.filebot.web.WebFixturesTest;

/**
 * Tests that must pass without network access. Run by {@code ant test} and blocking for the build.
 */
@RunWith(Suite.class)
@SuiteClasses({ TestEnvironmentTest.class, LauncherTest.class, WorkflowTest.class, JavaFxResidueTest.class, IconsTest.class, ThemeSwitchTest.class, ShortcutDocumentationTest.class, HistoryTest.class, RevertTest.class, XdgTrashTest.class, HistorySpoolerTest.class, PruneEmptyFoldersTest.class, PostProcessingTest.class, ScriptBundleTest.class, ScriptSourceTest.class, ExecCommandTest.class, GroovyPadCancelTest.class, ApplyDateTest.class, ConflictIndexTest.class, FileMapperTest.class, ArchiveExtractionTest.class, ExpressionFormatTest.class, ExpressionSandboxTest.class, HdrRegexTest.class, HttpsEndpointsTest.class, WebFixturesTest.class, ProgressMonitorEDTTest.class, ProgressMonitorCancelTest.class, ThemeDetectionTest.class, VerificationFormatTest.class, MatchModelTest.class, SupportDialogTest.class, EpisodeMetricsTest.class, ReleaseInfoTest.class, ReleaseInfoDataTest.class, VideoFormatTest.class, MediaDetectionTest.class, SimilarityTestSuite.class, DateMetricTest.class, SubtitleReaderTestSuite.class, UtilTestSuite.class, PropertyFileBackingStoreTest.class, SimpleDateTest.class })
public class OfflineTests {

}
