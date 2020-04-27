package com.quincy.auth;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quincy.auth.o.OAuth2TokenJWTPayload;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.RSASecurityHelper;

@Component
public class OAuth2ResourceHelper {
	public final static String RESOURCE_ERROR_MSG_KEY_PREFIX = "oauth2.error.resource.";
	@Resource(name = "primarySelfPublicKey")
	private PublicKey primarySelfPublicKey;
	@Resource(name = "secondarySelfPublicKey")
	private PublicKey secondarySelfPublicKey;
	@Value("${url.prefix.oauth2}")
	private String loginUriPrefix;

	public OAuth2Result validateToken(HttpServletRequest request, String accessToken, String _scope, String state, String locale) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException {
		String scope = CommonHelper.trim(_scope);
		if(scope==null)
			throw new RuntimeException("Value should be specified a valid string.");
		Integer errorStatus = null;
		String errorUri = null;
		String error = OAuthError.ResourceResponse.INVALID_REQUEST;
		String[] accessTokenFields = accessToken.split("\\.");
		if(accessTokenFields.length<3) {
			errorStatus = 3;
			error = OAuthError.ResourceResponse.INVALID_TOKEN;
		} else {
			String payload = accessTokenFields[1];
			String signature = accessTokenFields[2];
			boolean success = RSASecurityHelper.verify(primarySelfPublicKey, RSASecurityHelper.SIGNATURE_ALGORITHMS_SHA1_RSA, signature, accessTokenFields[0]+"."+payload, "UTF-8");
			if(!success)
				success = RSASecurityHelper.verify(secondarySelfPublicKey, RSASecurityHelper.SIGNATURE_ALGORITHMS_SHA1_RSA, signature, accessTokenFields[0]+"."+payload, "UTF-8");
			if(success) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				OAuth2TokenJWTPayload jwtPayload = mapper.readValue(Base64.getDecoder().decode(payload), OAuth2TokenJWTPayload.class);
				if(System.currentTimeMillis()>jwtPayload.getValidBefore()) {
					errorStatus = 5;
					error = OAuthError.ResourceResponse.EXPIRED_TOKEN;
				} else {
					String username = CommonHelper.trim(request.getParameter(OAuth.OAUTH_USERNAME));
					if(username==null) {
						errorStatus = 6;
					} else {
						boolean pass = false;
						List<String> accounts = jwtPayload.getAccounts();
						for(String account:accounts) {
							if(account.equals(username)) {
								pass = true;
								break;
							}
						}
						if(pass) {
							pass = false;
							List<String> scopes = jwtPayload.getScopes();
							for(String s:scopes) {
								if(s.equals(scope)) {
									pass = true;
									break;
								}
							}
							if(!pass) {
								errorStatus = 8;
								error = OAuthError.ResourceResponse.INSUFFICIENT_SCOPE;
								errorUri = CommonHelper.appendUriParam(CommonHelper.appendUriParam(new StringBuilder(100)
										.append(loginUriPrefix)
										.append("/oauth2/signin?")
										.append(OAuth.OAUTH_CLIENT_ID)
										.append("=")
										.append(jwtPayload.getClientId())
										.append("&")
										.append(OAuth.OAUTH_USERNAME)
										.append("=")
										.append(accounts.get(0))
										.append("&")
										.append(OAuth.OAUTH_SCOPE)
										.append("=")
										.append(scope), OAuth.OAUTH_STATE, state), InnerConstants.KEY_LOCALE, locale)
									.toString();
							}
						} else {
							errorStatus = 7;
							error = OAuthError.ResourceResponse.INVALID_TOKEN;
						}
					}
				}
			} else {
				errorStatus = 4;
				error = OAuthError.ResourceResponse.INVALID_TOKEN;
			}
		}
		OAuth2Result result = new OAuth2Result();
		result.setError(error);
		result.setErrorStatus(errorStatus);
		result.setErrorUri(errorUri);
		return result;
	}
}