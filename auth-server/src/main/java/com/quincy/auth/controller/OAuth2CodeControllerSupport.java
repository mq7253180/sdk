package com.quincy.auth.controller;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
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

import com.quincy.auth.controller.oauth2.OAuth2ControllerConstants;
import com.quincy.auth.controller.oauth2.OAuth2Template;
import com.quincy.auth.controller.oauth2.TemplateCustomization;
import com.quincy.auth.controller.oauth2.ValidationResult;
import com.quincy.auth.o.OAuth2Info;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

public abstract class OAuth2CodeControllerSupport {
	@Autowired
	private OAuth2Template oauth2Template;
	protected abstract ModelAndView signinView(HttpServletRequest request, String codeId, String scopes);
	protected abstract ModelAndView signinView(HttpServletRequest request, String clientId, String username, String scopes);
	protected abstract OAuth2Info getOAuth2Info(Long clientSystemId, String username, HttpServletRequest request);
	protected abstract String saveOAuth2Info(Long clientSystemId, Long userId, String authorizationCode);
	protected abstract List<String> notAuthorizedScopes(String codeId, Set<String> scopes);
	protected abstract boolean authenticateSecret(String inputed, String dbStored, String content);

	@RequestMapping("/signin/{code_id}")
	public ModelAndView signin(HttpServletRequest request, 
			@PathVariable(required = true, value = "code_id")String codeId, 
			@RequestParam(required = true, value = OAuth.OAUTH_SCOPE)String scopes, 
			@RequestParam(required = false, value = OAuth.OAUTH_REDIRECT_URI)String redirectUri) {
		ModelAndView mv = signinView(request, codeId, scopes);
		if(mv==null)
			mv = new ModelAndView("/oauth2_login");
		return mv.addObject("codeId", codeId)
				.addObject(OAuth.OAUTH_SCOPE, scopes)
				.addObject(OAuth.OAUTH_REDIRECT_URI, redirectUri);
	}

	@RequestMapping("/signin")
	public ModelAndView signin(HttpServletRequest request, 
			@RequestParam(required = true, value = OAuth.OAUTH_CLIENT_ID)String clientId, 
			@RequestParam(required = true, value = OAuth.OAUTH_USERNAME)String username, 
			@RequestParam(required = true, value = OAuth.OAUTH_SCOPE)String scopes, 
			@RequestParam(required = false, value = OAuth.OAUTH_REDIRECT_URI)String redirectUri) {
		ModelAndView mv = signinView(request, clientId, username, scopes);
		if(mv==null)
			mv = new ModelAndView("/oauth2_login");
		return mv.addObject(OAuth.OAUTH_SCOPE, scopes)
				.addObject(OAuth.OAUTH_REDIRECT_URI, redirectUri);
	}

	@RequestMapping("/code")
	public ResponseEntity<?> authorizationCode(HttpServletRequest request) throws OAuthSystemException, URISyntaxException {
		return oauth2Template.doTemplate(request, new TemplateCustomization() {
			@Override
			public ValidationResult authorize(OAuthRequest _oauthRequest, String _redirectUri, boolean isNotJson, String locale, String state, Long clientSystemId) throws OAuthSystemException, UnsupportedEncodingException {
				OAuthAuthzRequest oauthRequest = (OAuthAuthzRequest)_oauthRequest;
				Integer errorResponse = HttpServletResponse.SC_BAD_REQUEST;
				String error = OAuthError.CodeResponse.INVALID_REQUEST;
				String errorDescription = null;
				Integer errorStatus = null;
				String redirectUri = null;
				ValidationResult result = null;
				String username = CommonHelper.trim(oauthRequest.getParam(OAuth.OAUTH_USERNAME));
				Set<String> scopes = oauthRequest.getScopes();
				if(isNotJson&&_redirectUri==null) {
					errorStatus = 5;
				} if(username==null) {
					errorStatus = 6;
				} else if(scopes==null||scopes.size()==0) {
					errorStatus = 7;
				} else {
					OAuth2Info oauth2Info = getOAuth2Info(clientSystemId, username, request);
					if(oauth2Info.getUserId()==null) {
						errorStatus = 8;
					} else if(oauth2Info.getErrorMsg()!=null) {
						errorStatus = 21;
						errorDescription = oauth2Info.getErrorMsg();
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
					result = new ValidationResult();
					result.setRedirectUri(redirectUri);
					result.setErrorResponse(errorResponse);
					result.setError(error);
					result.setErrorStatus(errorStatus);
					result.setErrorDescription(errorDescription);
				}
				return result;
			}

			@Override
			public boolean authenticateSecretX(String inputed, String dbStored, String content) {
				return authenticateSecret(inputed, dbStored, content);
			}

			@Override
			public ValidationResult grant(OAuthRequest oauthRequest, String redirectUri, boolean isNotJson, String locale, String state, Long clientSystemId) {
				return null;
			}
		}, OAuth2ControllerConstants.REQ_CASE_CODE);
	}

	protected ResponseEntity<?> buildResponse(HttpServletRequest request, String authorizationCode) throws URISyntaxException, OAuthSystemException {
		String clientType = CommonHelper.clientType(request);
//		String clientType = InnerConstants.CLIENT_TYPE_J;
		boolean isNotJson = !InnerConstants.CLIENT_TYPE_J.equals(clientType);
		ValidationResult result =  buildResponse(request, isNotJson, CommonHelper.trim(request.getParameter(OAuth.OAUTH_REDIRECT_URI)), authorizationCode);
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

	private static ValidationResult buildResponse(HttpServletRequest request, boolean isNotJson, String redirectUri, String authorizationCode) throws OAuthSystemException {
		ValidationResult result = new ValidationResult();
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
}