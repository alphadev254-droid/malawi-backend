package mw.nwra.ewaterpermit.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//spring annotation
@Configuration
public class JasyptConfig {

//	@Value("${jasypt.encryptor.password}")
//	private static String key;

	// common method
	// used in classes - JasyptConfig.java and EncryptDecryptPwd.java
	public static SimpleStringPBEConfig getSimpleStringPBEConfig() {
		final SimpleStringPBEConfig pbeConfig = new SimpleStringPBEConfig();
		pbeConfig.setPassword("nyasad3v#s3cr3tk3y"); // encryptor private key
		pbeConfig.setAlgorithm("PBEWithMD5AndDES");
		pbeConfig.setKeyObtentionIterations("1000");
		pbeConfig.setPoolSize("1");
		pbeConfig.setProviderName("SunJCE");
		pbeConfig.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
		pbeConfig.setStringOutputType("base64");

		return pbeConfig;
	}

	@Bean(name = "jasyptStringEncryptor")
	public StringEncryptor encryptor() {
		final PooledPBEStringEncryptor pbeStringEncryptor = new PooledPBEStringEncryptor();
		pbeStringEncryptor.setConfig(getSimpleStringPBEConfig());

		return pbeStringEncryptor;
	}
}