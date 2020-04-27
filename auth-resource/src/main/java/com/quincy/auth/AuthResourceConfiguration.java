package com.quincy.auth;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.sdk.helper.RSASecurityHelper;

@Configuration
public class AuthResourceConfiguration {
	@Value("${secret.rsa.publicKey.primary}")
	private String primaryPublicKeyStr;
	@Value("${secret.rsa.publicKey.secondary}")
	private String secondaryPublicKeyStr;

	@Bean("primarySelfPublicKey")
	public PublicKey primaryPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		return RSASecurityHelper.extractPublicKeyByStr(primaryPublicKeyStr);
	}

	@Bean("secondarySelfPublicKey")
	public PublicKey secondaryPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		return RSASecurityHelper.extractPublicKeyByStr(secondaryPublicKeyStr);
	}
}