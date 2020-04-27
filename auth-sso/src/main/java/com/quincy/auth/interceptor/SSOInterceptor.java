package com.quincy.auth.interceptor;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.quincy.auth.OAuth2ResourceHelper;
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
			}
		}
		return true;
	}
}