package com.quincy.auth.interceptor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.quincy.auth.OAuth2ResourceHelper;
import com.quincy.auth.OAuth2Result;
import com.quincy.auth.annotation.OAuth2Resource;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SSOInterceptor extends HandlerInterceptorAdapter {
	@Autowired
	private OAuth2ResourceHelper oauth2ResourceHelper;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException, URISyntaxException, OAuthSystemException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			OAuth2Resource oauth2Resource = method.getMethod().getDeclaredAnnotation(OAuth2Resource.class);
			if(oauth2Resource!=null) {
				String scope = CommonHelper.trim(oauth2Resource.value());
				if(scope==null)
					throw new RuntimeException("Value should be specified a valid string.");
				String accessToken = null;
				String locale = CommonHelper.trim(request.getParameter(InnerConstants.KEY_LOCALE));
				String state = CommonHelper.trim(request.getParameter(OAuth.OAUTH_STATE));
				OAuth2Result result = oauth2ResourceHelper.validateToken(accessToken, scope, state, locale, null);
				if(result.getErrorStatus()!=null) {
					if(result.getErrorStatus()<5) {//跳错误页
						response.setStatus(result.getErrorResponse());
					} else {//跳授权、登录页
						
					}
				} else {//判断是否需要更新token，如果需要调接口更新
					
				}
				String clientType = CommonHelper.clientType(request);
//				String clientType = InnerConstants.CLIENT_TYPE_J;
				boolean isNotJson = !InnerConstants.CLIENT_TYPE_J.equals(clientType);
			}
		}
		return true;
	}
}