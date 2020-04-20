package com.quincy.auth.interceptor;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.quincy.auth.annotation.OAuth2Resource;
import com.quincy.sdk.helper.CommonHelper;

public class OAuth2ResourceInterceptor extends HandlerInterceptorAdapter {
	private Properties properties;

	public OAuth2ResourceInterceptor(Properties properties) {
		this.properties = properties;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws OAuthSystemException, OAuthProblemException {
		if(handler instanceof HandlerMethod) {
			boolean pass = false;
			HandlerMethod method = (HandlerMethod)handler;
			OAuth2Resource oauth2Resource = method.getMethod().getDeclaredAnnotation(OAuth2Resource.class);
			if(oauth2Resource!=null) {
				String scope = CommonHelper.trim(oauth2Resource.value());
				if(scope==null)
					throw new RuntimeException("Value should be specified a valid string.");
				OAuthAccessResourceRequest accessResourceRequest = new OAuthAccessResourceRequest(request);
				String accessToken = accessResourceRequest.getAccessToken();
			}
			return pass;
		} else
			return true;
	}
}