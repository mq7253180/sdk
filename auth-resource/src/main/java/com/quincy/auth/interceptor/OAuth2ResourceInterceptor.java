package com.quincy.auth.interceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;

import javax.annotation.Resource;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.quincy.auth.OAuth2ResourceConstants;
import com.quincy.auth.Oauth2Helper;
import com.quincy.auth.annotation.OAuth2Resource;
import com.quincy.auth.o.OAuth2TokenJWTPayload;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.RSASecurityHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2ResourceInterceptor extends HandlerInterceptorAdapter {
	@Resource(name = "selfPublicKey")
	private PublicKey publicKey;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException, URISyntaxException, OAuthSystemException {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			OAuth2Resource oauth2Resource = method.getMethod().getDeclaredAnnotation(OAuth2Resource.class);
			if(oauth2Resource!=null) {
				String scope = CommonHelper.trim(oauth2Resource.value());
				if(scope==null)
					throw new RuntimeException("Value should be specified a valid string.");
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
				mapper.setSerializationInclusion(Include.NON_NULL);
				String clientType = CommonHelper.clientType(request);
//				String clientType = InnerConstants.CLIENT_TYPE_J;
				boolean isNotJson = !InnerConstants.CLIENT_TYPE_J.equals(clientType);
				Integer errorStatus = null;
				String errorUri = null;
				OAuthResponseBuilder builder = null;
				try {
					String error = OAuthError.ResourceResponse.INVALID_REQUEST;
					OAuthAccessResourceRequest accessResourceRequest = new OAuthAccessResourceRequest(request);
					String accessToken = accessResourceRequest.getAccessToken();
					String[] accessTokenFields = accessToken.split("\\.");
					if(accessTokenFields.length<3) {
						errorStatus = 3;
						error = OAuthError.ResourceResponse.INVALID_TOKEN;
					} else {
						String payload = accessTokenFields[1];
						String signature = accessTokenFields[2];
						boolean success = RSASecurityHelper.verify(publicKey, RSASecurityHelper.SIGNATURE_ALGORITHMS_SHA1_RSA, signature, accessTokenFields[0]+"."+payload, "UTF-8");
						if(success) {
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
					if(errorStatus!=null)
						builder = OAuthRSResponse
								.errorResponse(isNotJson?HttpServletResponse.SC_FOUND:HttpServletResponse.SC_FORBIDDEN)
								.setError(error)
								.setErrorDescription(new RequestContext(request).getMessage(OAuth2ResourceConstants.RESOURCE_ERROR_MSG_KEY_PREFIX+errorStatus));
				} catch(Exception e) {
					log.error("OAUTH2_ERR_AUTHORIZATION: ", e);
					builder = OAuthRSResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST);
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
					String state = CommonHelper.trim(request.getParameter(OAuth.OAUTH_STATE));
					if(state!=null)
						builder.setParam(OAuth.OAUTH_STATE, state);
					if(errorUri==null)
						errorUri = Oauth2Helper.resourceErrorUri(errorStatus, CommonHelper.trim(request.getParameter(InnerConstants.KEY_LOCALE)));
					headers.setLocation(new URI(errorUri));
					builder.location(errorUri);
					((OAuthErrorResponseBuilder)builder).setErrorUri(errorUri);
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					OAuthResponse oauthResponse = null;
					if(isNotJson) {
						oauthResponse = builder.buildBodyMessage();
					} else {
						oauthResponse = builder.buildJSONMessage();
						headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
						response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
					}
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
					return false;
				}
			}
		}
		return true;
	}
}