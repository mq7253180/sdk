package com.quincy.auth.controller;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.security.auth.Subject;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.entity.ClientSystem;
import com.quincy.auth.o.DSession;
import com.quincy.auth.service.GeneralService;
import com.quincy.auth.service.OAuth2Service;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/oauth2")
public abstract class OAuth2ControllerSupport {
	@Autowired
	private GeneralService generalService;
	public abstract void saveInfo(String clientId, String scope, String authorizationCode, Long userId);
	public abstract String getAuthorizationCode(String clientId, String scope, String username);
	private final static String SIGNIN_URI = "/oauth2/signin";

	@RequestMapping("/code")
	public Object authorizationCode(HttpServletRequest request) throws OAuthSystemException, URISyntaxException {
		OAuthResponseBuilder builder = null;
		String uri = null;
		try {
			OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(request);
			ClientSystem clientSystem = generalService.findClientSystem(oauthRequest.getClientId());
			if(clientSystem==null) {
				builder = OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(OAuthError.TokenResponse.INVALID_CLIENT)
						.setErrorDescription("The client platform has not been registered into our platform.");
			} else {
				String authorizationCode = CommonHelper.trim(getAuthorizationCode(null, null, ""));
				builder = authorizationCode==null?OAuthASResponse
						.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
						.setError(OAuthError.CodeResponse.UNAUTHORIZED_CLIENT)
						.setErrorDescription("The client platform for this resource has not been authorized by the owner."):this.buildResponse(request, oauthRequest, authorizationCode);
			}
			uri = CommonHelper.trim(oauthRequest.getParam(OAuth.OAUTH_REDIRECT_URI));
		} catch(Exception e) {
			log.error("OAUTH2_ERR_AUTHORIZATION: ", e);
			builder = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST);
			if(e instanceof OAuthProblemException) {
				OAuthProblemException oauth2E = (OAuthProblemException)e;
				builder = ((OAuthErrorResponseBuilder)builder).error(oauth2E);
				uri = CommonHelper.trim(oauth2E.getRedirectUri());
			} else
				builder = ((OAuthErrorResponseBuilder)builder).setError(OAuthError.CodeResponse.SERVER_ERROR).setErrorDescription(e.getMessage());
		}
		HttpHeaders headers = new HttpHeaders();
		if(uri!=null) {
			builder = builder.location(uri);
			headers.setLocation(new URI(uri));
		}
		String clientType = CommonHelper.clientType(request);
//		String clientType = InnerConstants.CLIENT_TYPE_J;
		OAuthResponse response = null;
		if(InnerConstants.CLIENT_TYPE_J.equals(clientType)) {
			response = builder.buildJSONMessage();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		} else
			response = builder.buildBodyMessage();
		return new ResponseEntity<>(response.getBody(), headers, HttpStatus.valueOf(response.getResponseStatus()));
	}

	@RequestMapping("/error")
	public void error() {
		
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
	protected OAuthAuthorizationResponseBuilder generateAuthorizationCode(HttpServletRequest request, String clientId, String scope, Long userId) throws OAuthSystemException, OAuthProblemException {
		OAuthIssuer oauthIssuer = new OAuthIssuerImpl(new MD5Generator());
		String authorizationCode = oauthIssuer.authorizationCode();
		saveInfo(clientId, scope, authorizationCode, userId);
		return this.buildResponse(request, new OAuthAuthzRequest(request), authorizationCode);
	}

	private OAuthAuthorizationResponseBuilder buildResponse(HttpServletRequest request, OAuthAuthzRequest oauthRequest, String authorizationCode) throws OAuthSystemException {
		return OAuthASResponse
				.authorizationResponse(request, HttpServletResponse.SC_OK)
				.setCode(authorizationCode);
	}
}