package com.quincy.auth.controller;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.entity.ClientSystem;
import com.quincy.auth.o.OAuth2Info;
import com.quincy.auth.service.OAuth2Service;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/oauth2")
public abstract class OAuth2ControllerSupport {
	@Autowired
	private OAuth2Service oauth2Service;
	protected abstract OAuth2Info getOAuth2Info(Long clientSystemId, String username, String scope);
	protected abstract String saveOAuth2Info(Long clientSystemId, String username, String scope);
	protected abstract String saveOAuth2Info(Long clientSystemId, String username, String scope, String authorizationCode);
	protected abstract ModelAndView signinView(HttpServletRequest request, String oauth2Id);
	private final static String ERROR_URI = "/oauth2/error?status=";
	private final static String ERROR_MSG_KEY_PREFIX = "oauth2.error.";

	@Data
	private static class XxxResult {
		private Integer errorStatus = null;
		private Integer errorResponse = null;
		private String error = null;
		private String redirectUri = null;
		private OAuthResponseBuilder builder = null;
	}

	private interface Customization {
		public XxxResult doXXX(OAuthAuthzRequest oauthRequest, String redirectUri, boolean isNotJson, String locale, Long clientSystemId, String username, String scope) throws OAuthSystemException, UnsupportedEncodingException;
	}

	private ResponseEntity<?> doTemplate(HttpServletRequest request, Customization c) throws URISyntaxException, OAuthSystemException {
		String clientType = CommonHelper.clientType(request);
//		String clientType = InnerConstants.CLIENT_TYPE_J;
		boolean isNotJson = !InnerConstants.CLIENT_TYPE_J.equals(clientType);
		String locale = CommonHelper.trim(request.getParameter(InnerConstants.KEY_LOCALE));
		OAuthResponseBuilder builder = null;
		String redirectUri = null;
		Integer errorStatus = null;
		try {
			OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(request);
			String username = CommonHelper.trim(oauthRequest.getParam(OAuth.OAUTH_USERNAME));
			String scope = CommonHelper.trim(oauthRequest.getParam(OAuth.OAUTH_SCOPE));
			String _redirectUri = CommonHelper.trim(oauthRequest.getParam(OAuth.OAUTH_REDIRECT_URI));
			Integer errorResponse = HttpServletResponse.SC_BAD_REQUEST;
			String error = OAuthError.CodeResponse.INVALID_REQUEST;
			if(username==null) {
				errorStatus = 1;
			} else if(scope==null) {
				errorStatus = 2;
			} else if(isNotJson&&_redirectUri==null) {
				errorStatus = 3;
			} else {
				ClientSystem clientSystem = oauth2Service.findClientSystem(oauthRequest.getClientId());
				if(clientSystem==null) {
					errorStatus = 4;
				} else {
					XxxResult result = c.doXXX(oauthRequest, _redirectUri, isNotJson, locale, clientSystem.getId(), username, scope);
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
			builder = OAuthASResponse.errorResponse(isNotJson?HttpServletResponse.SC_FOUND:HttpServletResponse.SC_BAD_REQUEST);
			if(e instanceof OAuthProblemException) {
				OAuthProblemException oauth2E = (OAuthProblemException)e;
				builder = ((OAuthErrorResponseBuilder)builder).error(oauth2E);
				redirectUri = CommonHelper.trim(oauth2E.getRedirectUri());
				errorStatus = 5;
			} else {
				builder = ((OAuthErrorResponseBuilder)builder).setError(OAuthError.CodeResponse.SERVER_ERROR).setErrorDescription(e.getMessage());
				errorStatus = 6;
			}
		}
		OAuthResponse response = null;
		HttpHeaders headers = new HttpHeaders();
		if(isNotJson) {
			response = builder.buildBodyMessage();
			if(redirectUri==null) {
				redirectUri = appendLocale(new StringBuilder(100).append(ERROR_URI).append(errorStatus), locale).toString();
				builder = builder.location(redirectUri);
			}
		} else {
			response = builder.buildJSONMessage();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		}
		if(redirectUri!=null)
			headers.setLocation(new URI(redirectUri));
		return new ResponseEntity<>(response.getBody(), headers, HttpStatus.valueOf(response.getResponseStatus()));
	}

	@RequestMapping("/code")
	public ResponseEntity<?> authorizationCode(HttpServletRequest request) throws OAuthSystemException, URISyntaxException {
		return this.doTemplate(request, new Customization() {
			@Override
			public XxxResult doXXX(OAuthAuthzRequest oauthRequest, String _redirectUri, boolean isNotJson, String locale, Long clientSystemId, String username, String scope) throws OAuthSystemException, UnsupportedEncodingException {
				Integer errorStatus = null;
				Integer errorResponse = null;
				String error = null;
				String redirectUri = null;
				OAuthResponseBuilder builder = null;
				OAuth2Info oauth2Info = getOAuth2Info(clientSystemId, username, scope);
				XxxResult result = null;
				if(oauth2Info.getUserId()==null) {
					errorStatus = 11;
				} else {
					String authorizationCode = CommonHelper.trim(oauth2Info.getAuthorizationCode());
					if(authorizationCode==null) {
						errorResponse = HttpServletResponse.SC_UNAUTHORIZED;
						error = OAuthError.CodeResponse.UNAUTHORIZED_CLIENT;
						errorStatus = 12;
						String oauth2Id = saveOAuth2Info(clientSystemId, username, scope);
						StringBuilder s = appendLocale(new StringBuilder(150)//一般长度92
								.append("/oauth2/signin/")
								.append(oauth2Id)
							, locale);
						if(_redirectUri!=null)
							s = s.append(s.indexOf("?")<0?"?":"&")
								.append(OAuth.OAUTH_REDIRECT_URI)
								.append("=")
								.append(URLEncoder.encode(_redirectUri, "UTF-8"));
						redirectUri = s.toString();
					} else
						result = buildResponse(request, oauthRequest, isNotJson, authorizationCode, _redirectUri);
				}
				if(result==null) {
					result = new XxxResult();
					result.setRedirectUri(redirectUri);
					result.setBuilder(builder);
				}
				result.setErrorResponse(errorResponse);
				result.setError(error);
				result.setErrorStatus(errorStatus);
				return result;
			}
		});
	}

	private static StringBuilder appendLocale(StringBuilder s, String locale) {
		return locale==null?s:s.append(s.indexOf("?")<0?"?":"&")
				.append(InnerConstants.KEY_LOCALE)
				.append("=")
				.append(locale);
	}

	@RequestMapping("/error")
	public ModelAndView error(HttpServletRequest request, @RequestParam(required = true, value = "status")int status) {
		return new ModelAndView("/oauth2_error").addObject("msg", new RequestContext(request).getMessage(ERROR_MSG_KEY_PREFIX+status));
	}

	@RequestMapping("/signin/{oauth2_id}")
	public ModelAndView signin(HttpServletRequest request, 
			@PathVariable(required = true, value = "oauth2_id")String oauth2Id, 
			@RequestParam(required = false, value = OAuth.OAUTH_REDIRECT_URI)String redirectUri) {
		ModelAndView mv = signinView(request, oauth2Id);
		if(mv==null)
			mv = new ModelAndView("/oauth2_login");
		return mv.addObject(OAuth.OAUTH_REDIRECT_URI, redirectUri);
	}
	/**
	 * 生成授权码
	 */
	protected ResponseEntity<?> generateAuthorizationCode(HttpServletRequest request) throws URISyntaxException, OAuthSystemException {
		return this.doTemplate(request, new Customization() {
			@Override
			public XxxResult doXXX(OAuthAuthzRequest oauthRequest, String redirectUri, boolean isNotJson, String locale,
					Long clientSystemId, String username, String scope)
					throws OAuthSystemException, UnsupportedEncodingException {
				OAuthIssuer oauthIssuer = new OAuthIssuerImpl(new MD5Generator());
				String authorizationCode = oauthIssuer.authorizationCode();
				saveOAuth2Info(clientSystemId, username, scope, authorizationCode);
				return buildResponse(request, oauthRequest, isNotJson, authorizationCode, redirectUri);
			}
		});
	}

	private static XxxResult buildResponse(HttpServletRequest request, OAuthAuthzRequest oauthRequest, boolean isNotJson, String authorizationCode, String redirectUri) throws OAuthSystemException {
		XxxResult result = new XxxResult();
		result.setBuilder(OAuthASResponse
				.authorizationResponse(request, isNotJson?HttpServletResponse.SC_FOUND:HttpServletResponse.SC_OK)
				.setCode(authorizationCode));
		if(redirectUri!=null) {
			result.setRedirectUri(new StringBuilder(250)
					.append(redirectUri)
					.append("?")
					.append(OAuth.OAUTH_CODE)
					.append("=")
					.append(authorizationCode)
					.toString());
			log.info("LENGTH_2==========={}", result.getRedirectUri().length());
		}
		return result;
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
}