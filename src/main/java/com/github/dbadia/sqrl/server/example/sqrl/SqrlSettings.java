package com.github.dbadia.sqrl.server.example.sqrl;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlConfig;

/**
 * Simple example
 *
 * @author Dave Badia
 *
 */
public class SqrlSettings {
	private static final Logger	logger		= LoggerFactory.getLogger(SqrlSettings.class);
	private static SqrlConfig	sqrlConfig	= null;

	public static final SqrlConfig getSqrlConfig() {
		if (sqrlConfig == null) {
			// Check the classpath
			final InputStream is = SqrlSettings.class.getResourceAsStream("/sqrl.xml");
			if (is == null) {
				logger.warn("sqrl.xml file not found, falling back to default settings");
				sqrlConfig = getDemoSettings();
			} else {
				try {
					final JAXBContext jaxbContext = JAXBContext.newInstance(SqrlConfig.class);
					final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
					sqrlConfig = (SqrlConfig) jaxbUnmarshaller.unmarshal(is);
				} catch (final Exception e) {
					logger.error("Error unmarshalling sqrl.xml file", e);
					// Leave sqrlConfig null so we try again on the next request
				}
			}
		}
		return sqrlConfig;
	}

	private static final SqrlConfig getDemoSettings() {
		if (sqrlConfig == null) {
			sqrlConfig = new SqrlConfig();
			sqrlConfig.setBackchannelServletPath("sqrlbc");
			final SecureRandom secureRandom = new SecureRandom();
			final byte[] aesKeyBytes = new byte[16];
			secureRandom.nextBytes(aesKeyBytes);
			sqrlConfig.setAESKeyBytes(aesKeyBytes);
			sqrlConfig.setSecureRandom(secureRandom);
			sqrlConfig.setClientAuthStateUpdaterClass(
					"com.github.dbadia.sqrl.atmosphere.AtmosphereClientAuthStateUpdater");
		}

		return sqrlConfig;
	}

	/**
	 * @deprecated this is for test case use only, it has a hardcoded key and infinite timeout which are BAD practices
	 */
	@Deprecated
	private static final SqrlConfig getTestCaseSettings() {
		if (sqrlConfig == null) {
			sqrlConfig = new SqrlConfig();
			sqrlConfig.setServerFriendlyName("SQRL JUNIT test");
			sqrlConfig.setBackchannelServletPath("sqrlbc");
			sqrlConfig.setAESKeyBytes(new byte[16]);
			sqrlConfig.setNutValidityInSeconds(Integer.MAX_VALUE);
			sqrlConfig.setSecureRandom(new TestSecureRandom());
		}

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

	public static void main(final String[] args) {
		final SqrlConfig sqrlConfig = getDemoSettings();
		System.out.println(System.currentTimeMillis());
		try {
			final JAXBContext jaxbContext = JAXBContext.newInstance(SqrlConfig.class);
			final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(sqrlConfig, System.out);

		} catch (final JAXBException e) {
			e.printStackTrace();
		}
	}
}
