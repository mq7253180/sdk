package com.quincy.auth.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

@Component("authorizationInterceptor")
public class AuthorizationInterceptor extends AuthorizationInterceptorAbstract {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		return this.doAuth(request, response, handler, null);
	}
}
