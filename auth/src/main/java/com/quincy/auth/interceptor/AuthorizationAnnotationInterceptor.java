package com.quincy.auth.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import com.quincy.auth.annotation.LoginRequired;
import com.quincy.auth.annotation.PermissionNeeded;

@Component("authorizationAnnotationInterceptor")
public class AuthorizationAnnotationInterceptor extends AuthorizationInterceptorSupport {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			PermissionNeeded permissionNeededAnnotation = method.getMethod().getDeclaredAnnotation(PermissionNeeded.class);
			boolean permissionNeeded = permissionNeededAnnotation!=null;
			boolean loginRequired = method.getMethod().getDeclaredAnnotation(LoginRequired.class)!=null;
			return (!permissionNeeded&&!loginRequired)?true:this.doAuth(request, response, handler, permissionNeeded?permissionNeededAnnotation.value():null);
		} else
			return true;
	}
}