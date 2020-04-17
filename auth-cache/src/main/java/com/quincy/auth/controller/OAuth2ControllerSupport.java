package com.quincy.auth.controller;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.as.response.OAuthASResponse.OAuthAuthorizationResponseBuilder;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.OAuthResponse.OAuthErrorResponseBuilder;
import org.apache.oltu.oauth2.common.message.OAuthResponse.OAuthResponseBuilder;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.entity.ClientSystem;
import com.quincy.auth.o.OAuth2Info;
import com.quincy.auth.service.OAuth2Service;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/oauth2")
public abstract class OAuth2ControllerSupport {
	@Autowired
	private OAuth2Service oauth2Service;
	protected abstract OAuth2Info getOAuth2Info(Long clientSystemId, String username, String scope);
	protected abstract void saveInfo(Long clientSystemId, Long userId, String scope, String authorizationCode);
	protected abstract ModelAndView signinView(HttpServletRequest request);
	private final static String ERROR_URI = "/oauth2/error?status=";
	private final static String ERROR_MSG_KEY_PREFIX = "oauth2.error.";

	@RequestMapping("/code")
	public Object authorizationCode(HttpServletRequest request) throws OAuthSystemException, URISyntaxException {
		String clientType = CommonHelper.clientType(request);
//		String clientType = InnerConstants.CLIENT_TYPE_J;
		boolean isNotJson = !InnerConstants.CLIENT_TYPE_J.equals(clientType);
		OAuthResponseBuilder builder = null;
		String redirectUri = null;
		Integer errorStatus = null;
		try {
			OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(request);
			String username = CommonHelper.trim(oauthRequest.getParam(OAuth.OAUTH_USERNAME));
			String scpoe = CommonHelper.trim(oauthRequest.getParam(OAuth.OAUTH_SCOPE));
			Integer errorResponse = null;
			String error = null;
			if(username==null) {
				errorResponse = HttpServletResponse.SC_BAD_REQUEST;
				error = OAuthError.CodeResponse.INVALID_REQUEST;
				errorStatus = 1;
			} else if(scpoe==null) {
				errorResponse = HttpServletResponse.SC_BAD_REQUEST;
				error = OAuthError.CodeResponse.INVALID_REQUEST;
				errorStatus = 2;
			} else {
				ClientSystem clientSystem = oauth2Service.findClientSystem(oauthRequest.getClientId());
				if(clientSystem==null) {
					errorResponse = HttpServletResponse.SC_BAD_REQUEST;
					error = OAuthError.TokenResponse.INVALID_CLIENT;
					errorStatus = 3;
				} else {
					OAuth2Info oauth2Info = getOAuth2Info(clientSystem.getId(), username, scpoe);
					if(oauth2Info.getUserId()==null) {
						errorResponse = HttpServletResponse.SC_BAD_REQUEST;
						error = OAuthError.CodeResponse.INVALID_REQUEST;
						errorStatus = 4;
					} else {
						String authorizationCode = CommonHelper.trim(oauth2Info.getAuthorizationCode());
						if(authorizationCode==null) {
							errorResponse = HttpServletResponse.SC_UNAUTHORIZED;
							error = OAuthError.CodeResponse.UNAUTHORIZED_CLIENT;
							errorStatus = 5;
							redirectUri = new StringBuilder(100)
									.append("/oauth2/signin?userId=")
									.append(oauth2Info.getUserId())
									.toString();
							log.info("LENGTH_1==========={}", redirectUri.length());
						} else {
							redirectUri = new StringBuilder(250)
									.append(CommonHelper.trim(oauthRequest.getParam(OAuth.OAUTH_REDIRECT_URI)))
									.append("?")
									.append(OAuth.OAUTH_CODE)
									.append("=")
									.append(authorizationCode)
									.toString();
							log.info("LENGTH_2==========={}", redirectUri.length());
							builder = this.buildResponse(request, oauthRequest, isNotJson, authorizationCode);
						}
					}
				}
			}
			if(errorResponse!=null)
				builder = OAuthASResponse
				.errorResponse(isNotJson?HttpServletResponse.SC_FOUND:errorResponse)
				.setError(error)
				.setErrorDescription(new RequestContext(request).getMessage(ERROR_MSG_KEY_PREFIX+errorStatus));
		} catch(Exception e) {
			log.error("OAUTH2_ERR_AUTHORIZATION: ", e);
			builder = OAuthASResponse.errorResponse(isNotJson?HttpServletResponse.SC_FOUND:HttpServletResponse.SC_BAD_REQUEST);
			if(e instanceof OAuthProblemException) {
				OAuthProblemException oauth2E = (OAuthProblemException)e;
				builder = ((OAuthErrorResponseBuilder)builder).error(oauth2E);
				redirectUri = CommonHelper.trim(oauth2E.getRedirectUri());
				errorStatus = 6;
			} else {
				builder = ((OAuthErrorResponseBuilder)builder).setError(OAuthError.CodeResponse.SERVER_ERROR).setErrorDescription(e.getMessage());
				errorStatus = 7;
			}
		}
		if(redirectUri==null)
			redirectUri = ERROR_URI+errorStatus;
		String locale = CommonHelper.trim(request.getParameter(InnerConstants.KEY_LOCALE));
		if(locale!=null)
			redirectUri = new StringBuilder(100)//长度35
			.append(redirectUri)
			.append("&")
			.append(InnerConstants.KEY_LOCALE)
			.append("=")
			.append(locale)
			.toString();
		HttpHeaders headers = new HttpHeaders();
		builder = builder.location(redirectUri);
		headers.setLocation(new URI(redirectUri));
		OAuthResponse response = null;
		if(isNotJson) {
			response = builder.buildBodyMessage();
		} else {
			response = builder.buildJSONMessage();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		}
		return new ResponseEntity<>(response.getBody(), headers, HttpStatus.valueOf(response.getResponseStatus()));
	}

	@RequestMapping("/error")
	public ModelAndView error(HttpServletRequest request, @RequestParam(required = true, value = "status")int status) {
		return new ModelAndView("/oauth2_error").addObject("msg", new RequestContext(request).getMessage(ERROR_MSG_KEY_PREFIX+status));
	}

	@RequestMapping("/signin")
	public ModelAndView signin(HttpServletRequest request) {
		ModelAndView mv = signinView(request);
		if(mv==null)
			mv = new ModelAndView("/oauth2_login");
		return mv;
	}

	/*@RequestMapping("/token")
	public Object accessToken(HttpServletRequest request) {
		OAuthResponse response = null;
		try {
			OAuthTokenRequest oauthRequest = new OAuthTokenRequest(request);
			ClientSystem clientSystem = generalService.findClientSystem(oauthRequest.getClientId());
			if(clientSystem==null) {
				response = OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(OAuthError.TokenResponse.INVALID_CLIENT)
						.setErrorDescription("Client ID无效")
						.buildJSONMessage();
				return new ResponseEntity<>(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
			} else {
				
			}
		    //检查客户端安全KEY是否正确
			DSession session = oauth2Service.getSession(accessToken);
			if (!oauthService.checkClientSecret(oauthRequest.getClientSecret())) {
				response = OAuthASResponse
						.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
						.setError(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT)
						.setErrorDescription(Constants.INVALID_CLIENT_DESCRIPTION)
						.buildJSONMessage();
				return new ResponseEntity<>(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
			}
			String authCode = oauthRequest.getParam(OAuth.OAUTH_CODE);
			//检查验证类型，此处只检查AUTHORIZATION_CODE类型，其他的还有PASSWORD或REFRESH_TOKEN
			if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(GrantType.AUTHORIZATION_CODE.toString())) {
				if (!oAuthService.checkAuthCode(authCode)) {
					OAuthResponse response = OAuthASResponse
							.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
							.setError(OAuthError.TokenResponse.INVALID_GRANT)
							.setErrorDescription("错误的授权码")
							.buildJSONMessage();
					return new ResponseEntity<>(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
				}
			}
			//生成Access Token
			OAuthIssuer oauthIssuer = new OAuthIssuerImpl(new MD5Generator());
			final String accessToken = oauthIssuer.accessToken();
			oAuthService.addAccessToken(accessToken, oAuthService.getUsernameByAuthCode(authCode));
			//生成OAuth响应
			OAuthResponse response = OAuthASResponse
					.tokenResponse(HttpServletResponse.SC_OK)
					.setAccessToken(accessToken)
					.setExpiresIn(String.valueOf(oAuthService.getExpireIn()))
					.buildJSONMessage();
			return new ResponseEntity<>(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
		} catch(OAuthProblemException e) {
			//构建错误响应
			OAuthResponse res = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(e).buildJSONMessage();
			return new ResponseEntity<>(res.getBody(), HttpStatus.valueOf(res.getResponseStatus()));
		}
	}*/
	/**
	 * 生成授权码
	 */
	protected OAuthAuthorizationResponseBuilder generateAuthorizationCode(HttpServletRequest request, Long clientSystemId, Long userId, String scope) throws OAuthSystemException, OAuthProblemException {
		OAuthIssuer oauthIssuer = new OAuthIssuerImpl(new MD5Generator());
		String authorizationCode = oauthIssuer.authorizationCode();
		saveInfo(clientSystemId, userId, scope, authorizationCode);
		return this.buildResponse(request, new OAuthAuthzRequest(request), true, authorizationCode);
	}

	private OAuthAuthorizationResponseBuilder buildResponse(HttpServletRequest request, OAuthAuthzRequest oauthRequest, boolean isNotJson, String authorizationCode) throws OAuthSystemException {
		return OAuthASResponse
				.authorizationResponse(request, isNotJson?HttpServletResponse.SC_FOUND:HttpServletResponse.SC_OK)
				.setCode(authorizationCode);
	}
}