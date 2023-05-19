package com.quincy.auth.interceptor;

import java.lang.reflect.Method;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import com.quincy.auth.annotation.LoginRequired;
import com.quincy.auth.annotation.PermissionNeeded;
import com.quincy.sdk.annotation.KeepCookieIfExpired;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component("authorizationAnnotationInterceptor")
public class AuthorizationAnnotationInterceptor extends AuthorizationInterceptorSupport {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod)handler;
			Method method = handlerMethod.getMethod();
			PermissionNeeded permissionNeededAnnotation = method.getDeclaredAnnotation(PermissionNeeded.class);
			boolean permissionNeeded = permissionNeededAnnotation!=null;
			boolean loginRequired = method.getDeclaredAnnotation(LoginRequired.class)!=null;
			if(!permissionNeeded&&!loginRequired) {
				boolean deleteCookieIfExpired = method.getDeclaredAnnotation(KeepCookieIfExpired.class)==null;
				this.setExpiry(request, deleteCookieIfExpired);
				return true;
			} else
				return this.doAuth(request, response, handler, permissionNeeded?permissionNeededAnnotation.value():null);
		} else
			return true;
	}
}