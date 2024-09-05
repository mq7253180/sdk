package com.quincy.auth.interceptor;

import java.lang.reflect.Method;

import org.springframework.web.method.HandlerMethod;

import com.quincy.sdk.annotation.auth.LoginRequired;
import com.quincy.sdk.annotation.auth.PermissionNeeded;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthorizationAnnotationInterceptor extends AuthorizationInterceptorSupport {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod)handler;
			Method method = handlerMethod.getMethod();
			PermissionNeeded permissionNeededAnnotation = method.getDeclaredAnnotation(PermissionNeeded.class);
			boolean permissionNeeded = permissionNeededAnnotation!=null;
			boolean loginRequired = method.getDeclaredAnnotation(LoginRequired.class)!=null;
			if(!loginRequired&&!permissionNeeded) {
//				boolean deleteCookieIfExpired = method.getDeclaredAnnotation(KeepCookieIfExpired.class)==null;
//				this.setExpiry(request, deleteCookieIfExpired);
				return true;
			} else
				return this.doAuth(request, response, handler, permissionNeeded?permissionNeededAnnotation.value():null);
		} else
			return true;
	}
}