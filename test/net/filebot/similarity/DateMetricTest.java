package net.filebot.similarity;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Test;

/**
 * Tests for {@link DateMetric} similarity (FIX-23 / BUG-25).
 *
 * <p>DateMetric returns {@code 1} for matching dates, {@code -1} for
 * non-matching dates (penalty used by the matcher), and {@code 0} when
 * either date is unknown.
 */
public class DateMetricTest {

	DateMetric metric = new DateMetric(new DateMatcher(DateMatcher.DEFAULT_SANITY, Locale.ENGLISH));

	@Test
	public void matchingDatesReturnOne() {
		assertEquals(1, metric.getSimilarity("2008-02-10", "The Daily Show [10.2.2008] Lou Dobbs"), 0);
		assertEquals(1, metric.getSimilarity("2008-04-03", "The Daily Show - 2008.04.03 - George Clooney"), 0);
	}

	@Test
	public void nonMatchingDatesReturnPenalty() {
		// BUG-25: DateMetric returns -1 (penalty) for different dates, not 0
		assertEquals(-1, metric.getSimilarity("2008-01-01", "The Daily Show [10.2.2008] Lou Dobbs"), 0);
		assertEquals(-1, metric.getSimilarity("2008-01-01", "The Daily Show - 2008.04.03 - George Clooney"), 0);
	}

	@Test
	public void unknownDatesReturnZero() {
		assertEquals(0, metric.getSimilarity("not a date", "not a date either"), 0);
	}

}
