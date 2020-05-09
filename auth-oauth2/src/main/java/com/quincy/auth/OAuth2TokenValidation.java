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
import javax.servlet.http.HttpServletResponse;

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
public class OAuth2TokenValidation {
	@Resource(name = "primarySelfPublicKey")
	private PublicKey primarySelfPublicKey;
	@Resource(name = "secondarySelfPublicKey")
	private PublicKey secondarySelfPublicKey;
	@Value("${url.prefix.oauth2}")
	private String centerUrlPrefix;

	private String invalidTokenError;
	private String expiredTokenError;
	private String invalidRequestError;
	private String insufficientScopeError;

	public OAuth2Result validateToken(String accessToken, String _scope, String state, String locale, HttpServletRequest request) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException {
		String scope = CommonHelper.trim(_scope);
		if(scope==null)
			throw new RuntimeException("Value should be specified a valid string.");
		Integer errorStatus = null;
		String errorUri = null;
		String error = null;
		Integer errorResponse = null;
		OAuth2Result result = new OAuth2Result();
		String[] accessTokenFields = accessToken.split("\\.");
		if(accessTokenFields.length<3) {
			errorStatus = 3;
			error = invalidTokenError;
			errorResponse = HttpServletResponse.SC_BAD_REQUEST;
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
				if(System.currentTimeMillis()>jwtPayload.getExpirationTime()) {
					errorStatus = 5;
					error = expiredTokenError;
					errorResponse = HttpServletResponse.SC_FORBIDDEN;
				} else {
					List<String> accounts = jwtPayload.getAccounts();
					if(request!=null) {
						String username = CommonHelper.trim(request.getParameter(OAuth2Constants.OAUTH_USERNAME));
						if(username==null) {
							errorStatus = 6;
							error = invalidRequestError;
							errorResponse = HttpServletResponse.SC_BAD_REQUEST;
						} else {
							boolean pass = false;
							for(String account:accounts) {
								if(account.equals(username)) {
									pass = true;
									break;
								}
							}
							if(!pass) {
								errorStatus = 7;
								error = invalidTokenError;
								errorResponse = HttpServletResponse.SC_FORBIDDEN;
							}
						}
					}
					if(errorStatus==null) {
						boolean pass = false;
						List<String> scopes = jwtPayload.getScopes();
						for(String s:scopes) {
							if(s.equals(scope)) {
								pass = true;
								if(request==null) {//更新token
									long expire = jwtPayload.getExpirationTime()-jwtPayload.getIssuedAtTime();
									long remaining = jwtPayload.getExpirationTime()-System.currentTimeMillis();
									if(remaining<(expire/2)) {
										errorStatus = 0;
										result.setUsername(accounts.get(0));
									}
								}
								break;
							}
						}
						if(!pass) {
							errorStatus = 8;
							error = insufficientScopeError;
							errorResponse = HttpServletResponse.SC_FORBIDDEN;
							errorUri = CommonHelper.appendUriParam(CommonHelper.appendUriParam(new StringBuilder(100)
									.append("_self".equals(centerUrlPrefix)?"":centerUrlPrefix)
									.append("/oauth2/signin?")
									.append(OAuth2Constants.OAUTH_CLIENT_ID)
									.append("=")
									.append(jwtPayload.getClientId())
									.append("&")
									.append(OAuth2Constants.OAUTH_USERNAME)
									.append("=")
									.append(accounts.get(0))
									.append("&")
									.append(OAuth2Constants.OAUTH_SCOPE)
									.append("=")
									.append(scope), OAuth2Constants.OAUTH_STATE, state), InnerConstants.KEY_LOCALE, locale)
								.toString();
						}
					}
				}
			} else {
				errorStatus = 4;
				error = invalidTokenError;
				errorResponse = HttpServletResponse.SC_FORBIDDEN;
			}
		}
		result.setError(error);
		result.setErrorResponse(errorResponse);
		result.setErrorStatus(errorStatus);
		result.setErrorUri(errorUri);
		return result;
	}

	public void setInvalidTokenError(String invalidTokenError) {
		this.invalidTokenError = invalidTokenError;
	}
	public void setExpiredTokenError(String expiredTokenError) {
		this.expiredTokenError = expiredTokenError;
	}
	public void setInvalidRequestError(String invalidRequestError) {
		this.invalidRequestError = invalidRequestError;
	}
	public void setInsufficientScopeError(String insufficientScopeError) {
		this.insufficientScopeError = insufficientScopeError;
	}
}