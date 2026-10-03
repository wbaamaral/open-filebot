package net.filebot.format;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

/**
 * Tests for {@code {hdr}} binding regexes (FIX-17 / BUG-16).
 *
 * <p>Verifies that HDR10+ is recognized from filenames (where {@code \b} fails
 * after {@code +}), DV is restricted to video contexts, and HLG is detected.
 */
public class HdrRegexTest {

	private String hdr(String filename) {
		MediaBindingBean bean = new MediaBindingBean(null, new File(filename));
		return bean.getHDR();
	}

	// --- HDR10+ (BUG-16: \b fails after +) ---

	@Test
	public void hdr10PlusWithDotAfterPlus() {
		assertEquals("HDR10+", hdr("Movie.2020.2160p.HDR10+.mkv"));
	}

	@Test
	public void hdr10PlusWithHEVCAfterPlus() {
		assertEquals("HDR10+", hdr("Movie.2020.2160p.HDR10+.HEVC.mkv"));
	}

	@Test
	public void hdr10PlusWordForm() {
		assertEquals("HDR10+", hdr("Movie.2020.HDR10Plus.mkv"));
	}

	// --- HDR10 (without +) ---

	@Test
	public void hdr10WithoutPlus() {
		assertEquals("HDR10", hdr("Movie.2020.2160p.HDR10.mkv"));
	}

	@Test
	public void hdrBare() {
		assertEquals("HDR10", hdr("Movie.2020.2160p.HDR.mkv"));
	}

	// --- Dolby Vision (restricted DV) ---

	@Test
	public void dolbyVisionFullName() {
		assertEquals("Dolby Vision", hdr("Movie.2020.Dolby.Vision.mkv"));
	}

	@Test
	public void dolbyVisionDoVi() {
		assertEquals("Dolby Vision", hdr("Movie.2020.DoVi.mkv"));
	}

	@Test
	public void dvInVideoContext() {
		assertEquals("Dolby Vision", hdr("Movie.2020.DV.HDR.mkv"));
	}

	@Test
	public void dvRipIsNotDolbyVision() {
		assertNotEquals("Dolby Vision", hdr("Movie.2020.DV-rip.mkv"));
	}

	@Test
	public void dvrIsNotDolbyVision() {
		assertNotEquals("Dolby Vision", hdr("Movie.2020.DVR.mkv"));
	}

	// --- HLG ---

	@Test
	public void hlgFromFilename() {
		assertEquals("HLG", hdr("Movie.2020.HLG.mkv"));
	}

	// --- null when no HDR ---

	@Test
	public void noHdrReturnsNull() {
		assertNull(hdr("Movie.2020.1080p.mkv"));
	}

}
