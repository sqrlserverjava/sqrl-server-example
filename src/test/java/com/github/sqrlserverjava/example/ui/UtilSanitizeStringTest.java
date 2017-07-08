package com.github.sqrlserverjava.example.ui;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.sqrlserverjava.example.Util;

@RunWith(value = Parameterized.class)
public class UtilSanitizeStringTest {
	private static final int MAX_LENGTH = 10;

	@Parameter(value = 0)
	public String inputString;
	@Parameter(value = 1)
	public String expectedString;

	// Single parameter, use Object[]
	@Parameters(name = "{index}: input={0}, expected={1}")
	// @formatter:off
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
			{"abc", "abc"},
			{"abc1", "abc1"},
			{"abc1?", "abc1"},
			// Max length tests
			{"abcdefghij", "abcdefghij"},
			{"abcdefghijklmo", "abcdefghij"},
		});
	}
	// @formatter:on

	@Test
	public void testIt() {
		assertEquals(expectedString, Util.sanitizeString(inputString, MAX_LENGTH));
	}
}
