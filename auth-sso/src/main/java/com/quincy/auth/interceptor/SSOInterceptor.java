package com.quincy.auth.interceptor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.OAuth.HttpMethod;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quincy.auth.OAuth2Constants;
import com.quincy.auth.OAuth2TokenValidation;
import com.quincy.auth.OAuth2Result;
import com.quincy.auth.Oauth2Helper;
import com.quincy.auth.annotation.UserSession;
import com.quincy.core.InnerConstants;
import com.quincy.core.InnerHelper;
import com.quincy.core.ThreadLocalHolder;
import com.quincy.sdk.helper.CommonHelper;

import lombok.Data;

@Component
public class SSOInterceptor extends HandlerInterceptorAdapter {
	@Value("${url.prefix.oauth2}")
	private String centerUrlPrefix;
	@Value("${oauth2.clientId}")
	private String clientId;
	@Value("${oauth2.clientSecret}")
	private String clientSecret;
	@Value("${clientTokenName.sso}")
	private String clientTokenName;
	@Autowired
	private OAuth2TokenValidation oauth2TokenValidation;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException, URISyntaxException, OAuthSystemException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchMessageException, ServletException, OAuthProblemException {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			UserSession annotation = method.getMethod().getDeclaredAnnotation(UserSession.class);
			if(annotation!=null) {
				/*
				 * 获取token
				 * 判断是token是否过期
				 * 	1如果过期跳登录页或吐json超时状态；
				 * 	2如果离过期少于一半时间调中心系统重新获取token并刷新token
				 *	3如果离过期时间超一半调中心系统获取session对象注入给参数
				 */
				String accessToken = CommonHelper.getValue(request, clientTokenName);
				if(accessToken==null) {//跳登录页
					this.handleTimeout(request, response, handler);
					return false;
				} else {
					String locale = CommonHelper.trim(request.getParameter(InnerConstants.KEY_LOCALE));
					String state = CommonHelper.trim(request.getParameter(OAuth.OAUTH_STATE));
					OAuth2Result result = oauth2TokenValidation.validateToken(accessToken, OAuth2Constants.SCOPE_NAME_USER_INFO, null, null, request);
					if(result.getErrorStatus()!=null) {
						if(result.getErrorStatus()==0) {//判断是否需要更新token，如果需要调接口更新
							OAuthClientRequest oauthClientRequest = OAuthClientRequest
									.authorizationLocation(new StringBuilder(200)
											.append(centerUrlPrefix)
											.append("/oauth2/code")
											.toString())
									.setResponseType(ResponseType.CODE.toString())
									.setClientId(clientId)
									.setScope(OAuth2Constants.SCOPE_NAME_USER_INFO)
									.setState(state)
//									.setRedirectURI(redirectUrl)
									.setParameter(OAuth.OAUTH_CLIENT_SECRET, clientSecret)
									.setParameter(InnerConstants.CLIENT_APP, "sso")
									.setParameter(OAuth.OAUTH_USERNAME, result.getUsername())
									.buildBodyMessage();
							OAuthClient client = new OAuthClient(new URLConnectionClient());
							OAuthCodeClientResponse codeResponse = client.resource(oauthClientRequest, HttpMethod.POST, OAuthCodeClientResponse.class);
							oauthClientRequest = OAuthClientRequest
									.tokenLocation(new StringBuilder(200)
											.append(centerUrlPrefix)
											.append("/oauth2/token")
											.toString())
									.setGrantType(GrantType.AUTHORIZATION_CODE)
									.setClientId(clientId)
									.setClientSecret(clientSecret)
									.setCode(codeResponse.getCode())
//									.setRedirectURI(redirectUrl)
									.setParameter(InnerConstants.CLIENT_APP, "sso")
									.buildBodyMessage();
							OAuthAccessTokenResponse oauthAccessTokenResponse = client.accessToken(oauthClientRequest, OAuthJSONAccessTokenResponse.class);
							String newAccessToken = oauthAccessTokenResponse.getAccessToken();
							ThreadLocalHolder.setAccsessToken(newAccessToken);
							/*
							 * 将newAccessToken写到cookie里
							 * 反序列化session对象，放到session里
							 */
						} else if(result.getErrorStatus()<5) {//Token被窜改过，跳错误页
							response.setStatus(result.getErrorResponse());
							return false;
						} else {
							if(result.getErrorStatus()==5) {//会话超时、跳登录页
								this.handleTimeout(request, response, handler);
							} else if(result.getErrorStatus()==8)//权限不足(基本不会)
								InnerHelper.outputOrForward(request, response, handler, -1, new RequestContext(request).getMessage("status.error.403")+"["+OAuth2Constants.SCOPE_NAME_USER_INFO+"]", centerUrlPrefix+Oauth2Helper.resourceErrorUri(result.getErrorStatus(), locale), true);
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	private void handleTimeout(HttpServletRequest request, HttpServletResponse response, Object handler) throws NoSuchMessageException, IOException, ServletException {
		InnerHelper.outputOrForward(request, response, handler, 0, new RequestContext(request).getMessage("oauth2.error.sso.timeout"), centerUrlPrefix+"/oauth2/signin", true);
	}

	private class OAuthCodeClientResponse extends OAuthClientResponse {
		public String getCode() throws JsonParseException, JsonMappingException, IOException {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			OAuth2Code oauth2Code = mapper.readValue(body, OAuth2Code.class);
			return oauth2Code.getCode();
		}

		@Override
		protected void setContentType(String contentType) {
			
		}

		@Override
		protected void setResponseCode(int responseCode) {
			
		}
	}

	@Data
	private class OAuth2Code {
		private String code;
	}
}