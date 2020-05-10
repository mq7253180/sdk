package com.quincy.auth.controller.oauth2;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.Oauth2Helper;
import com.quincy.auth.entity.ClientSystem;
import com.quincy.auth.service.OAuth2Service;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2Template {
	@Autowired
	private OAuth2Service oauth2Service;

	public ResponseEntity<?> doTemplate(HttpServletRequest request, TemplateCustomization templateCustomization, int reqCase) throws URISyntaxException, OAuthSystemException {
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
			String errorDescription = null;
			String _redirectUri = CommonHelper.trim(request.getParameter(OAuth.OAUTH_REDIRECT_URI));
			OAuthRequest oauthRequest = reqCase==OAuth2ControllerConstants.REQ_CASE_CODE?new OAuthAuthzRequest(request):new OAuthTokenRequest(request);
			ClientSystem clientSystem = oauth2Service.findClientSystem(oauthRequest.getClientId());
			if(clientSystem==null) {
				errorStatus = 3;
			} else {
				String _secret = CommonHelper.trim(oauthRequest.getClientSecret());
				String secret = CommonHelper.trim(clientSystem.getSecret());
				if(_secret==null||!templateCustomization.authenticateSecret(_secret, secret, "")) {
					errorStatus = 4;
				} else {
					ValidationResult result = reqCase==OAuth2ControllerConstants.REQ_CASE_CODE?templateCustomization.authorize(oauthRequest, _redirectUri, isNotJson, locale, state, clientSystem.getId()):templateCustomization.grant(oauthRequest, _redirectUri, isNotJson, locale, state, clientSystem.getId());
					errorResponse = result.getErrorResponse();
					error = result.getError();
					errorDescription = result.getErrorDescription();
					errorStatus = result.getErrorStatus();
					redirectUri = result.getRedirectUri();
					builder =  result.getBuilder();
				}
			}
			if(builder==null)
				builder = OAuthASResponse
						.errorResponse(isNotJson?HttpServletResponse.SC_FOUND:errorResponse)
						.setError(error)
						.setErrorDescription(errorDescription==null?new RequestContext(request).getMessage(OAuth2ControllerConstants.ERROR_MSG_KEY_PREFIX+errorStatus):errorDescription);
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
}