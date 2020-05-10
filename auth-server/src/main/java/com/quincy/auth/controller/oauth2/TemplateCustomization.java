package com.quincy.auth.controller.oauth2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import org.apache.oltu.oauth2.as.request.OAuthRequest;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

public interface TemplateCustomization {
	public boolean authenticateSecret(String inputed, String dbStored, String content);
	public ValidationResult authorize(OAuthRequest oauthRequest, String redirectUri, boolean isNotJson, String locale, String state, Long clientSystemId) throws OAuthSystemException, UnsupportedEncodingException;
	public ValidationResult grant(OAuthRequest oauthRequest, String redirectUri, boolean isNotJson, String locale, String state, Long clientSystemId) throws OAuthSystemException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException;
}