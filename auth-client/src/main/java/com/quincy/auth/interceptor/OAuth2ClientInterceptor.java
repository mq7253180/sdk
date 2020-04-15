package com.quincy.auth.interceptor;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.quincy.auth.annotation.LoginRequired;

public class OAuth2ClientInterceptor extends HandlerInterceptorAdapter {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws ServletException, IOException {
		if(handler instanceof HandlerMethod) {
			boolean pass = false;
			HandlerMethod method = (HandlerMethod)handler;
			boolean loginRequired = method.getMethod().getDeclaredAnnotation(LoginRequired.class)!=null;
			if(loginRequired) {
				
			}
			return pass;
		} else
			return true;
	}
}