package mw.nwra.ewaterpermit.util;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

import mw.nwra.ewaterpermit.config.JasyptConfig;

public class EncryptDecryptPwd {

	// encrypt the plan text
	public static String encryptKey(final String plainKey) {
		final SimpleStringPBEConfig pbeConfig = JasyptConfig.getSimpleStringPBEConfig();
		final PooledPBEStringEncryptor pbeStringEncryptor = new PooledPBEStringEncryptor();
		pbeStringEncryptor.setConfig(pbeConfig);

		return pbeStringEncryptor.encrypt(plainKey);
	}

	// decrypt the encrypted text
	public static String decryptKey(final String encryptedKey) {
		final SimpleStringPBEConfig pbeConfig = JasyptConfig.getSimpleStringPBEConfig();
		final PooledPBEStringEncryptor pbeStringEncryptor = new PooledPBEStringEncryptor();
		pbeStringEncryptor.setConfig(pbeConfig);

		return pbeStringEncryptor.decrypt(encryptedKey);
	}
}
