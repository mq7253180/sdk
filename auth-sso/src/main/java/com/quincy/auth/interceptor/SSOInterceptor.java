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
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.OAuth2ResourceHelper;
import com.quincy.auth.OAuth2Result;
import com.quincy.auth.Oauth2Helper;
import com.quincy.auth.annotation.OAuth2Resource;
import com.quincy.core.InnerConstants;
import com.quincy.core.InnerHelper;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SSOInterceptor extends HandlerInterceptorAdapter {
	@Autowired
	private OAuth2ResourceHelper oauth2ResourceHelper;
	@Value("${ssoTokenName}")
	private String ssoTokenName;
	@Value("${url.prefix.oauth2}")
	private String centralUrlPrefix;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException, URISyntaxException, OAuthSystemException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchMessageException, ServletException {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			OAuth2Resource oauth2Resource = method.getMethod().getDeclaredAnnotation(OAuth2Resource.class);
			if(oauth2Resource!=null) {
				String scope = CommonHelper.trim(oauth2Resource.value());
				if(scope==null)
					throw new RuntimeException("Value should be specified a valid string.");
				String clientType = CommonHelper.clientType(request);
//				String clientType = InnerConstants.CLIENT_TYPE_J;
				boolean isNotJson = !InnerConstants.CLIENT_TYPE_J.equals(clientType);
				String accessToken = CommonHelper.getValue(request, ssoTokenName);
				if(accessToken==null) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return false;
				} else {
					String locale = CommonHelper.trim(request.getParameter(InnerConstants.KEY_LOCALE));
					String state = CommonHelper.trim(request.getParameter(OAuth.OAUTH_STATE));
					OAuth2Result result = oauth2ResourceHelper.validateToken(accessToken, scope, state, locale, null);
					if(result.getErrorStatus()!=null) {
						if(result.getErrorStatus()==0) {//判断是否需要更新token，如果需要调接口更新
							OAuthClient client = new OAuthClient(new URLConnectionClient());
							/*OAuthClientRequest oauthClientRequest = OAuthClientRequest
									.tokenLocation(userCenterTokenUrl)
									.setClientId(clientId)
									.setClientSecret(clientSecret)
									.setRedirectURI(redirectUrl)
									.setGrantType(GrantType.AUTHORIZATION_CODE)
									.setCode(accessCode)
									.buildBodyMessage();
							String newAccessToken = client.accessToken(request, OAuthJSONAccessTokenResponse.class).getAccessToken();*/
						} else if(result.getErrorStatus()<5) {//跳错误页
							response.setStatus(result.getErrorResponse());
							return false;
						} else {
							Integer status = null;
							String location = null;
							String msg = null;
							if(result.getErrorStatus()==5) {//会话超时、跳登录页
								status = 0;
								msg = new RequestContext(request).getMessage("oauth2.error.sso.timeout");
								location = centralUrlPrefix+"/auth/signin/broker";
							} else if(result.getErrorStatus()==8) {//权限不足
								status = -1;
								msg = new RequestContext(request).getMessage("status.error.403")+"["+scope+"]";
								location = Oauth2Helper.resourceErrorUri(result.getErrorStatus(), locale);
							}
							InnerHelper.outputOrForward(request, response, handler, status, msg, location, InnerHelper.APPEND_BACKTO_FLAG_URL);
							return false;
						}
					}
				}
			}
		}
		return true;
	}
}