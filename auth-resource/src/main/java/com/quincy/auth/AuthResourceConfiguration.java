package com.quincy.auth;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.annotation.PostConstruct;

import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.springframework.beans.factory.annotation.Autowired;
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
	@Autowired
	private OAuth2ResourceHelper oauth2ResourceHelper;

	@Bean("primarySelfPublicKey")
	public PublicKey primaryPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		return RSASecurityHelper.extractPublicKeyByStr(primaryPublicKeyStr);
	}

	@Bean("secondarySelfPublicKey")
	public PublicKey secondaryPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		return RSASecurityHelper.extractPublicKeyByStr(secondaryPublicKeyStr);
	}

	@PostConstruct
	public void init() {
		oauth2ResourceHelper.setInvalidTokenError(OAuthError.ResourceResponse.INVALID_TOKEN);
		oauth2ResourceHelper.setExpiredTokenError(OAuthError.ResourceResponse.EXPIRED_TOKEN);
		oauth2ResourceHelper.setInvalidRequestError(OAuthError.ResourceResponse.INVALID_REQUEST);
		oauth2ResourceHelper.setInsufficientScopeError(OAuthError.ResourceResponse.INSUFFICIENT_SCOPE);
		OAuth2Constants.OAUTH_CLIENT_ID = OAuth.OAUTH_CLIENT_ID;
		OAuth2Constants.OAUTH_SCOPE = OAuth.OAUTH_SCOPE;
		OAuth2Constants.OAUTH_STATE = OAuth.OAUTH_SCOPE;
		OAuth2Constants.OAUTH_USERNAME = OAuth.OAUTH_USERNAME;
	}
}