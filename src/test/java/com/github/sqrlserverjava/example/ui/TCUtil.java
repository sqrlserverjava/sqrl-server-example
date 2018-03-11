package com.github.sqrlserverjava.example.ui;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import com.github.sqrlserverjava.SqrlConfig;

public class TCUtil {
	static final String AES_TEST_KEY = Base64.getEncoder().encodeToString(new byte[16]);
	
	/**
	 * @deprecated this is for test case use only, it has a hardcoded key and infinite timeout which are BAD practices
	 */
	@Deprecated
	private static final SqrlConfig getTestCaseSettings() {
		final SqrlConfig sqrlConfig = new SqrlConfig();
		sqrlConfig.setServerFriendlyName("SQRL JUNIT test");
		sqrlConfig.setBackchannelServletPath("sqrlbc");
		sqrlConfig.setAesKeyBase64(AES_TEST_KEY);
		sqrlConfig.setNutValidityInSeconds(Integer.MAX_VALUE);
		sqrlConfig.setSecureRandom(new TestSecureRandom());

		return sqrlConfig;
	}

	/**
	 * A SecureRandom which isn't random at all.
	 *
	 * @author Dave Badia
	 * @deprecated Test case data generation ONLY
	 */
	@Deprecated
	private static class TestSecureRandom extends SecureRandom {
		@Override
		synchronized public void nextBytes(final byte[] bytes) {
			Arrays.fill(bytes, (byte) 0);
		}
	}

}
