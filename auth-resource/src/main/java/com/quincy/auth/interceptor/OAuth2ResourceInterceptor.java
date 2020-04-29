package com.quincy.auth.interceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.OAuthResponse.OAuthErrorResponseBuilder;
import org.apache.oltu.oauth2.common.message.OAuthResponse.OAuthResponseBuilder;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.OAuth2Constants;
import com.quincy.auth.OAuth2ResourceHelper;
import com.quincy.auth.OAuth2Result;
import com.quincy.auth.Oauth2Helper;
import com.quincy.auth.annotation.OAuth2Resource;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2ResourceInterceptor extends HandlerInterceptorAdapter {
	@Autowired
	private OAuth2ResourceHelper oauth2ResourceHelper;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException, URISyntaxException, OAuthSystemException {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			OAuth2Resource oauth2Resource = method.getMethod().getDeclaredAnnotation(OAuth2Resource.class);
			if(oauth2Resource!=null) {
				String scope = CommonHelper.trim(oauth2Resource.value());
				if(scope==null)
					throw new RuntimeException("Value should be specified a valid string.");
				String locale = CommonHelper.trim(request.getParameter(InnerConstants.KEY_LOCALE));
				String state = CommonHelper.trim(request.getParameter(OAuth.OAUTH_STATE));
				String clientType = CommonHelper.clientType(request);
//				String clientType = InnerConstants.CLIENT_TYPE_J;
				boolean isNotJson = !InnerConstants.CLIENT_TYPE_J.equals(clientType);
				Integer errorResponse = null;
				Integer errorStatus = null;
				String errorUri = null;
				OAuthResponseBuilder builder = null;
				try {
					OAuthAccessResourceRequest accessResourceRequest = new OAuthAccessResourceRequest(request);
					String accessToken = accessResourceRequest.getAccessToken();
					OAuth2Result result = oauth2ResourceHelper.validateToken(accessToken, scope, state, locale, request);
					errorStatus = result.getErrorStatus();
					errorUri = result.getErrorUri();
					if(errorStatus!=null) {
						errorResponse = isNotJson?HttpServletResponse.SC_FOUND:result.getErrorResponse();
						builder = OAuthRSResponse
								.errorResponse(errorResponse)
								.setError(result.getError())
								.setErrorDescription(new RequestContext(request).getMessage(OAuth2Constants.RESOURCE_ERROR_MSG_KEY_PREFIX+errorStatus));
					}
				} catch(Exception e) {
					log.error("OAUTH2_ERR_AUTHORIZATION: ", e);
					errorResponse = HttpServletResponse.SC_BAD_REQUEST;
					builder = OAuthRSResponse.errorResponse(errorResponse);
					if(e instanceof OAuthProblemException) {
						OAuthProblemException oauth2E = (OAuthProblemException)e;
						((OAuthErrorResponseBuilder)builder).error(oauth2E).setError(OAuthError.ResourceResponse.INVALID_REQUEST);
						errorStatus = 1;
						errorUri = CommonHelper.trim(oauth2E.getRedirectUri());
					} else {
						((OAuthErrorResponseBuilder)builder)
								.setError(OAuthError.CodeResponse.SERVER_ERROR)
								.setErrorDescription(e.getMessage());
						errorStatus = 2;
					}
				}
				if(builder!=null) {
					HttpHeaders headers = new HttpHeaders();
					if(state!=null)
						builder.setParam(OAuth.OAUTH_STATE, state);
					if(errorUri==null)
						errorUri = Oauth2Helper.resourceErrorUri(errorStatus, locale);
					headers.setLocation(new URI(errorUri));
					builder.location(errorUri);
					((OAuthErrorResponseBuilder)builder).setErrorUri(errorUri);
					OAuthResponse oauthResponse = null;
					if(isNotJson) {
						oauthResponse = builder.buildBodyMessage();
						response.setStatus(errorResponse==null?HttpServletResponse.SC_FORBIDDEN:errorResponse);
					} else {
						oauthResponse = builder.buildJSONMessage();
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);
						headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
						response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
					}
					if(errorResponse==HttpServletResponse.SC_FOUND) {
						response.sendRedirect(errorUri);
					} else {
						ResponseEntity<?> responseEntity = new ResponseEntity<>(oauthResponse.getBody(), headers, HttpStatus.valueOf(oauthResponse.getResponseStatus()));
						PrintWriter out = null;
						try {
							out = response.getWriter();
							out.println(responseEntity.getBody().toString());
						} catch(Exception e) {
							log.error("OAUTH2_OUT_ERR: ", e);
						} finally {
							if(out!=null)
								out.close();
						}
					}
					return false;
				}
			}
		}
		return true;
	}
}