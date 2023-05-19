package com.quincy.auth.interceptor;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component("authorizationInterceptor")
public class AuthorizationInterceptor extends AuthorizationInterceptorSupport {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		return this.doAuth(request, response, handler, null);
	}
}
