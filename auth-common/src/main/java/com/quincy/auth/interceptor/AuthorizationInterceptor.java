package com.quincy.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthorizationInterceptor extends AuthorizationInterceptorSupport {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		return this.doAuth(request, response, handler, null);
	}
}
