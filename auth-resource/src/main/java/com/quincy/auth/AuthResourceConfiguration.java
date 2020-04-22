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
	@Value("${secret.rsa.publicKey}")
	private String publicKeyStr;

	@Bean("publicKey")
	public PublicKey publicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		return RSASecurityHelper.extractPublicKeyByStr(publicKeyStr);
	}
}