package com.quincy.auth.interceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.AuthHelper;
import com.quincy.auth.o.XSession;
import com.quincy.core.InnerHelper;
import com.quincy.sdk.annotation.CustomizedInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@CustomizedInterceptor(pathPatterns = "/**")
public class MultiEnterpriseInterceptor extends HandlerInterceptorAdapter {
	@Value("${auth.center:}")
	private String authCenter;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		XSession xsession = AuthHelper.getSession(request);
		if(xsession.getUser().getCurrentEnterprise()==null) {
			InnerHelper.outputOrRedirect(request, response, handler, -8, new RequestContext(request).getMessage("auth.noenterprise"), authCenter+"/failure", true);
			return false;
		} else
			return true;
	}
}