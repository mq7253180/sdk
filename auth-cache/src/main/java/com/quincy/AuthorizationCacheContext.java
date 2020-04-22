package com.quincy;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.sdk.helper.RSASecurityHelper;

@Configuration
public class AuthorizationCacheContext {
	@Value("${spring.application.name}")
	private String applicationName;

	@Bean("sessionKeyPrefix")
	public String sessionKeyPrefix() {
		return applicationName+".session.";
	}

	@Value("${secret.rsa.privateKey}")
	private String privateKeyStr;

	@Bean("privateKey")
	public PrivateKey privateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		return RSASecurityHelper.extractPrivateKey(privateKeyStr);
	}
}