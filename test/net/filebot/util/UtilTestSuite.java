package net.filebot.util;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.filebot.util.ui.TokensTest;

@RunWith(Suite.class)
@SuiteClasses({ TokensTest.class, FileUtilitiesTest.class, ByteBufferOutputStreamTest.class, PreferencesMapTest.class, PreferencesListTest.class, TreeIteratorTest.class, FilterIteratorTest.class, StringUtilitiesTest.class })
public class UtilTestSuite {

}
