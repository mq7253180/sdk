package com.quincy.auth.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.OAuthResponse.OAuthErrorResponseBuilder;
import org.apache.oltu.oauth2.common.message.OAuthResponse.OAuthResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.quincy.auth.Oauth2Helper;
import com.quincy.auth.entity.ClientSystem;
import com.quincy.auth.o.OAuth2Info;
import com.quincy.auth.o.Oauth2Token;
import com.quincy.auth.o.Oauth2TokenInfo;
import com.quincy.auth.service.OAuth2Service;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.RSASecurityHelper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/oauth2")
public abstract class OAuth2ControllerSupport {
	@Autowired
	private OAuth2Service oauth2Service;
	protected abstract OAuth2Info getOAuth2Info(Long clientSystemId, String username);
	protected abstract OAuth2Info getOAuth2Info(String authorizationCode);
	protected abstract String saveOAuth2Info(Long clientSystemId, Long userId, String authorizationCode);
	protected abstract List<String> notAuthorizedScopes(String codeId, Set<String> scopes);
	protected abstract ModelAndView signinView(HttpServletRequest request, String codeId, String scopes);
	protected abstract long accessTokenExpireMillis();
	protected abstract int refreshTokenExpireDays();
	protected abstract boolean authenticateSecret(String inputed, String dbStored);
	private final static String ERROR_MSG_KEY_PREFIX = "oauth2.error.server.";
	private final static int REQ_CASE_CODE = 0;
	private final static int REQ_CASE_TOKEN = 1;

	@Data
	private static class XxxResult {
		private Integer errorStatus = null;
		private Integer errorResponse = null;
		private String error = null;
		private String redirectUri = null;
		private OAuthResponseBuilder builder = null;
	}

	private interface Customization {
		public XxxResult authorize(OAuthRequest oauthRequest, String redirectUri, boolean isNotJson, String locale, String state, Long clientSystemId) throws OAuthSystemException, UnsupportedEncodingException;
		public XxxResult grant(OAuthRequest oauthRequest, String redirectUri, boolean isNotJson, String locale, String state) throws OAuthSystemException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException;
	}

	private ResponseEntity<?> doTemplate(HttpServletRequest request, Customization c, int reqCase) throws URISyntaxException, OAuthSystemException {
		String clientType = CommonHelper.clientType(request);
//		String clientType = InnerConstants.CLIENT_TYPE_J;
		boolean isNotJson = !InnerConstants.CLIENT_TYPE_J.equals(clientType);
		String locale = CommonHelper.trim(request.getParameter(InnerConstants.KEY_LOCALE));
		String state = CommonHelper.trim(request.getParameter(OAuth.OAUTH_STATE));
		OAuthResponseBuilder builder = null;
		String redirectUri = null;
		Integer errorStatus = null;
		String errorUri = null;
		try {
			Integer errorResponse = HttpServletResponse.SC_BAD_REQUEST;
			String error = OAuthError.CodeResponse.INVALID_REQUEST;
			String _redirectUri = CommonHelper.trim(request.getParameter(OAuth.OAUTH_REDIRECT_URI));
			OAuthRequest oauthRequest = reqCase==REQ_CASE_CODE?new OAuthAuthzRequest(request):new OAuthTokenRequest(request);
			ClientSystem clientSystem = oauth2Service.findClientSystem(oauthRequest.getClientId());
			if(clientSystem==null) {
				errorStatus = 3;
			} else {
				String _secret = CommonHelper.trim(oauthRequest.getClientSecret());
				String secret = CommonHelper.trim(clientSystem.getSecret());
				if(_secret==null||!this.authenticateSecret(_secret, secret)) {
					errorStatus = 4;
				} else {
					XxxResult result = reqCase==REQ_CASE_CODE?c.authorize(oauthRequest, _redirectUri, isNotJson, locale, state, clientSystem.getId()):c.grant(oauthRequest, _redirectUri, isNotJson, locale, state);
					errorResponse = result.getErrorResponse();
					error = result.getError();
					errorStatus = result.getErrorStatus();
					redirectUri = result.getRedirectUri();
					builder =  result.getBuilder();
				}
			}
			if(builder==null)
				builder = OAuthASResponse
						.errorResponse(isNotJson?HttpServletResponse.SC_FOUND:errorResponse)
						.setError(error)
						.setErrorDescription(new RequestContext(request).getMessage(ERROR_MSG_KEY_PREFIX+errorStatus));
		} catch(Exception e) {
			log.error("OAUTH2_ERR_AUTHORIZATION: ", e);
			builder = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST);
			if(e instanceof OAuthProblemException) {
				OAuthProblemException oauth2E = (OAuthProblemException)e;
				((OAuthErrorResponseBuilder)builder).error(oauth2E);
				errorStatus = 1;
				errorUri = CommonHelper.trim(oauth2E.getRedirectUri());
			} else {
				((OAuthErrorResponseBuilder)builder)
						.setError(OAuthError.CodeResponse.SERVER_ERROR)
						.setErrorDescription(e.getMessage());
				errorStatus = 2;
			}
		}
		HttpHeaders headers = new HttpHeaders();
		if(state!=null)
			builder.setParam(OAuth.OAUTH_STATE, state);
		if(redirectUri==null&&errorStatus!=null)
			redirectUri = errorUri==null?Oauth2Helper.serverErrorUri(errorStatus, locale):errorUri;
		if(redirectUri!=null) {
			headers.setLocation(new URI(redirectUri));
			builder.location(redirectUri);
			if(builder instanceof OAuthErrorResponseBuilder)
				((OAuthErrorResponseBuilder)builder).setErrorUri(redirectUri);
		}
		OAuthResponse response = null;
		if(isNotJson) {
			response = builder.buildBodyMessage();
		} else {
			response = builder.buildJSONMessage();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		}
		return new ResponseEntity<>(response.getBody(), headers, HttpStatus.valueOf(response.getResponseStatus()));
	}

	@RequestMapping("/code")
	public ResponseEntity<?> authorizationCode(HttpServletRequest request) throws OAuthSystemException, URISyntaxException {
		return this.doTemplate(request, new Customization() {
			@Override
			public XxxResult authorize(OAuthRequest oauthRequest, String _redirectUri, boolean isNotJson, String locale, String state, Long clientSystemId) throws OAuthSystemException, UnsupportedEncodingException {
				Integer errorResponse = HttpServletResponse.SC_BAD_REQUEST;
				String error = OAuthError.CodeResponse.INVALID_REQUEST;
				Integer errorStatus = null;
				String redirectUri = null;
				XxxResult result = null;
				String username = CommonHelper.trim(oauthRequest.getParam(OAuth.OAUTH_USERNAME));
				Set<String> scopes = oauthRequest.getScopes();
				if(isNotJson&&_redirectUri==null) {
					errorStatus = 5;
				} if(username==null) {
					errorStatus = 6;
				} else if(scopes==null||scopes.size()==0) {
					errorStatus = 7;
				} else {
					OAuth2Info oauth2Info = getOAuth2Info(clientSystemId, username);
					if(oauth2Info.getUserId()==null) {
						errorStatus = 8;
					} else {
						String codeId = CommonHelper.trim(oauth2Info.getId());
						String authorizationCode = CommonHelper.trim(oauth2Info.getAuthorizationCode());
						if(authorizationCode==null) {
							OAuthIssuer oauthIssuer = new OAuthIssuerImpl(new MD5Generator());
							authorizationCode = oauthIssuer.authorizationCode();
							codeId = saveOAuth2Info(clientSystemId, oauth2Info.getUserId(), authorizationCode);
						}
						List<String> notAuthorizedScopes = notAuthorizedScopes(codeId, scopes);
						if(notAuthorizedScopes!=null&&notAuthorizedScopes.size()>0) {
							errorResponse = HttpServletResponse.SC_UNAUTHORIZED;
							error = OAuthError.CodeResponse.UNAUTHORIZED_CLIENT;
							errorStatus = 9;
							StringBuilder s = CommonHelper.appendUriParam(new StringBuilder(150)//一般长度92
									.append("/oauth2/signin/")
									.append(codeId)
									.append("?")
									.append(OAuth.OAUTH_SCOPE)
									.append("=")
									.append(oauthRequest.getParam(OAuth.OAUTH_SCOPE))
								, InnerConstants.KEY_LOCALE, locale);
							s = CommonHelper.appendUriParam(s, OAuth.OAUTH_STATE, state);
							s = CommonHelper.appendUriParam(s, OAuth.OAUTH_REDIRECT_URI, _redirectUri);
							redirectUri = s.toString();
						} else
							result = buildResponse(request, isNotJson, _redirectUri, authorizationCode);
					}
				}
				if(result==null) {
					result = new XxxResult();
					result.setRedirectUri(redirectUri);
					result.setErrorResponse(errorResponse);
					result.setError(error);
					result.setErrorStatus(errorStatus);
				}
				return result;
			}

			@Override
			public XxxResult grant(OAuthRequest oauthRequest, String redirectUri, boolean isNotJson, String locale, String state) {
				return null;
			}
		}, REQ_CASE_CODE);
	}

	@RequestMapping("/error/server")
	public ModelAndView error(HttpServletRequest request, @RequestParam(required = true, value = "status")int status) {
		return new ModelAndView("/oauth2_error").addObject("msg", new RequestContext(request).getMessage(ERROR_MSG_KEY_PREFIX+status));
	}

	@RequestMapping("/signin/{code_id}")
	public ModelAndView signin(HttpServletRequest request, 
			@PathVariable(required = true, value = "code_id")String codeId, 
			@RequestParam(required = true, value = "scope")String scopes, 
			@RequestParam(required = false, value = OAuth.OAUTH_REDIRECT_URI)String redirectUri) {
		ModelAndView mv = signinView(request, codeId, scopes);
		if(mv==null)
			mv = new ModelAndView("/oauth2_login");
		return mv.addObject("codeId", codeId)
				.addObject(OAuth.OAUTH_SCOPE, scopes)
				.addObject(OAuth.OAUTH_REDIRECT_URI, redirectUri);
	}

	protected ResponseEntity<?> buildResponse(HttpServletRequest request, String authorizationCode) throws URISyntaxException, OAuthSystemException {
		String clientType = CommonHelper.clientType(request);
//		String clientType = InnerConstants.CLIENT_TYPE_J;
		boolean isNotJson = !InnerConstants.CLIENT_TYPE_J.equals(clientType);
		XxxResult result =  buildResponse(request, isNotJson, CommonHelper.trim(request.getParameter(OAuth.OAUTH_REDIRECT_URI)), authorizationCode);
		OAuthResponseBuilder builder = result.getBuilder();
		String redirectUri = result.getRedirectUri();
		OAuthResponse response = null;
		HttpHeaders headers = new HttpHeaders();
		if(isNotJson) {
			response = builder.buildBodyMessage();
		} else {
			response = builder.buildJSONMessage();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		}
		if(redirectUri!=null)
			headers.setLocation(new URI(redirectUri));
		return new ResponseEntity<>(response.getBody(), headers, HttpStatus.valueOf(response.getResponseStatus()));
	}

	private static XxxResult buildResponse(HttpServletRequest request, boolean isNotJson, String redirectUri, String authorizationCode) throws OAuthSystemException {
		XxxResult result = new XxxResult();
		result.setBuilder(OAuthASResponse
				.authorizationResponse(request, isNotJson?HttpServletResponse.SC_FOUND:HttpServletResponse.SC_OK)
				.setCode(authorizationCode));
		if(redirectUri!=null) {
			StringBuilder s = CommonHelper.appendUriParam(new StringBuilder(100)//一般长度46
					.append(redirectUri)
					.append("?")
					.append(OAuth.OAUTH_CODE)
					.append("=")
					.append(authorizationCode), OAuth.OAUTH_STATE, CommonHelper.trim(request.getParameter(OAuth.OAUTH_STATE)));
			result.setRedirectUri(s.toString());
		}
		return result;
	}

	@Resource(name = "selfPrivateKey")
	private PrivateKey privateKey;

	@RequestMapping("/token")
	public Object accessToken(HttpServletRequest request) throws URISyntaxException, OAuthSystemException {
		return this.doTemplate(request, new Customization() {
			@Override
			public XxxResult grant(OAuthRequest oauthRequest, String redirectUri, boolean isNotJson, String locale, String state) throws OAuthSystemException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException {
				String clientId = oauthRequest.getClientId();
				XxxResult result = new XxxResult();
				OAuth2Info info = getOAuth2Info(oauthRequest.getParam(OAuth.OAUTH_CODE));
				if(!clientId.equals(info.getClientId())) {
					result.setErrorResponse(HttpServletResponse.SC_FORBIDDEN);
					result.setError(OAuthError.TokenResponse.INVALID_GRANT);
					result.setErrorStatus(10);
				} else {
					ObjectMapper mapper = new ObjectMapper();
					mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
					mapper.setSerializationInclusion(Include.NON_NULL);
					long currentTimeMillis = System.currentTimeMillis();
					Oauth2TokenInfo tokenInfo = new Oauth2TokenInfo();
					tokenInfo.setClientId(clientId);
					tokenInfo.setUserId(info.getUserId());
					tokenInfo.setScopes(info.getScopes());
					tokenInfo.setValidBefore(currentTimeMillis+accessTokenExpireMillis());
					String json = mapper.writeValueAsString(tokenInfo);
					String signature = RSASecurityHelper.sign(privateKey, RSASecurityHelper.SIGNATURE_ALGORITHMS_SHA1_RSA, "UTF-8", json);
					Oauth2Token token = new Oauth2Token();
					token.setInfo(tokenInfo);
					token.setSignature(signature);
					json = mapper.writeValueAsString(token);
					String accessToken = Base64.getEncoder().encodeToString(json.getBytes());
					tokenInfo.setValidBefore(currentTimeMillis+(refreshTokenExpireDays()*24*3600*1000));
					json = mapper.writeValueAsString(tokenInfo);
					signature = RSASecurityHelper.sign(privateKey, RSASecurityHelper.SIGNATURE_ALGORITHMS_SHA1_RSA, "UTF-8", json);
					token.setSignature(signature);
					json = mapper.writeValueAsString(token);
					String refreshToken = Base64.getEncoder().encodeToString(json.getBytes());
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
			public XxxResult authorize(OAuthRequest oauthRequest, String redirectUri, boolean isNotJson, String locale, String state, Long clientSystemId) throws OAuthSystemException, UnsupportedEncodingException {
				return null;
			}
		}, REQ_CASE_TOKEN);
	}
}