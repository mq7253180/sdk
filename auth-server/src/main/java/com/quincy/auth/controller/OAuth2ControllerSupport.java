package com.quincy.auth.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Base64.Encoder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.as.request.OAuthRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.quincy.auth.controller.oauth2.OAuth2ControllerConstants;
import com.quincy.auth.controller.oauth2.OAuth2Template;
import com.quincy.auth.controller.oauth2.TemplateCustomization;
import com.quincy.auth.controller.oauth2.ValidationResult;
import com.quincy.auth.o.OAuth2Info;
import com.quincy.auth.o.OAuth2TokenJWTPayload;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.RSASecurityHelper;

@RequestMapping("/oauth2")
public abstract class OAuth2ControllerSupport {
	@Autowired
	private OAuth2Template oauth2Template;
	protected abstract OAuth2Info getOAuth2Info(String authorizationCode);
	protected abstract long accessTokenExpireMillis();
	protected abstract int refreshTokenExpireDays();
	protected abstract boolean authenticateSecret(String inputed, String dbStored, String content);
	private static String JWT_HEADER = null;

	static {
		JWT_HEADER = Base64.getEncoder().encodeToString(new StringBuilder(50)
				.append("{ \"alg\": \"")
				.append(RSASecurityHelper.SIGNATURE_ALGORITHMS_SHA1_RSA)
				.append("\", \"typ\": \"JWT\"}")
				.toString()
				.getBytes()
			);
	}

	@RequestMapping("/error/server")
	public ModelAndView error(HttpServletRequest request, @RequestParam(required = true, value = "status")int status) {
		return new ModelAndView("/oauth2_error").addObject("msg", new RequestContext(request).getMessage(OAuth2ControllerConstants.ERROR_MSG_KEY_PREFIX+status));
	}

	@Resource(name = "selfPrivateKey")
	private PrivateKey privateKey;

	@RequestMapping("/token")
	public Object accessToken(HttpServletRequest request) throws URISyntaxException, OAuthSystemException {
		return oauth2Template.doTemplate(request, new TemplateCustomization() {
			@Override
			public ValidationResult grant(OAuthRequest _oauthRequest, String redirectUri, boolean isNotJson, String locale, String state, Long clientSystemId) throws OAuthSystemException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException {
				OAuthTokenRequest oauthRequest = (OAuthTokenRequest)_oauthRequest;
				String clientId = oauthRequest.getClientId();
				OAuth2Info info = getOAuth2Info(oauthRequest.getCode());
				ValidationResult result = new ValidationResult();
				if(!clientId.equals(info.getClientId())) {
					result.setErrorResponse(HttpServletResponse.SC_FORBIDDEN);
					result.setError(OAuthError.TokenResponse.INVALID_GRANT);
					result.setErrorStatus(10);
				} else {
					result = new ValidationResult();
					ObjectMapper mapper = new ObjectMapper();
					mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
					mapper.setSerializationInclusion(Include.NON_NULL);
					Encoder encoder = Base64.getEncoder();
					long currentTimeMillis = System.currentTimeMillis();
					OAuth2TokenJWTPayload payload = new OAuth2TokenJWTPayload();
					payload.setClientId(clientId);
					payload.setAccounts(info.getAccounts());
					payload.setScopes(info.getScopes());
					payload.setIssuedAtTime(currentTimeMillis);
					payload.setExpirationTime(currentTimeMillis+accessTokenExpireMillis());
					String accessToken = generateToken(mapper, encoder, privateKey, payload);
					payload.setExpirationTime(currentTimeMillis+(refreshTokenExpireDays()*24*3600*1000));
					String refreshToken = generateToken(mapper, encoder, privateKey, payload);
					result.setBuilder(OAuthASResponse
							.tokenResponse(isNotJson?HttpServletResponse.SC_FOUND:HttpServletResponse.SC_OK)
							.setAccessToken(accessToken)
							.setRefreshToken(refreshToken)
							.setExpiresIn(String.valueOf(accessTokenExpireMillis()))
						);
					if(redirectUri!=null)
						result.setRedirectUri(CommonHelper.appendUriParam(new StringBuilder(100)//一般长度46
								.append(redirectUri)
								.append("?")
								.append(OAuth.OAUTH_ACCESS_TOKEN)
								.append("=")
								.append(accessToken)
								.append("&")
								.append(OAuth.OAUTH_REFRESH_TOKEN)
								.append("=")
								.append(refreshToken)
								.append("&")
								.append(OAuth.OAUTH_EXPIRES_IN)
								.append("=")
								.append(accessTokenExpireMillis())
							, OAuth.OAUTH_STATE, state).toString());
				}
				return result;
			}

			@Override
			public boolean authenticateSecretX(String inputed, String dbStored, String content) {
				return authenticateSecret(inputed, dbStored, content);
			}

			@Override
			public ValidationResult authorize(OAuthRequest oauthRequest, String redirectUri, boolean isNotJson, String locale, String state, Long clientSystemId) throws OAuthSystemException, UnsupportedEncodingException {
				return null;
			}
		}, OAuth2ControllerConstants.REQ_CASE_TOKEN);
	}

	private static String generateToken(ObjectMapper mapper, Encoder encoder, PrivateKey privateKey, OAuth2TokenJWTPayload payload) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException {
		String payloadJsonAndBase64 = encoder.encodeToString(mapper.writeValueAsString(payload).getBytes());
		String headerAndPayload = JWT_HEADER+"."+payloadJsonAndBase64;
		String signature = RSASecurityHelper.sign(privateKey, RSASecurityHelper.SIGNATURE_ALGORITHMS_SHA1_RSA, "UTF-8", headerAndPayload);
		return headerAndPayload+"."+signature;
	}
}