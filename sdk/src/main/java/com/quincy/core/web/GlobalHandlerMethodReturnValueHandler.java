package com.quincy.core.web;

import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.quincy.sdk.Result;
import com.quincy.sdk.annotation.DoNotWrap;
import com.quincy.sdk.helper.CommonHelper;

public class GlobalHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {
	private HandlerMethodReturnValueHandler origin;
	private ApplicationContext applicationContext;
	private String[] noWrapperUris;

	public GlobalHandlerMethodReturnValueHandler(HandlerMethodReturnValueHandler origin, ApplicationContext applicationContext, String[] noWrapperUris) {
		this.origin = origin;
		this.applicationContext = applicationContext;
		this.noWrapperUris = noWrapperUris;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return origin.supportsReturnType(returnType);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws Exception {
		DoNotWrap doNotWrap = returnType.getMethod().getDeclaredAnnotation(DoNotWrap.class);
		if(doNotWrap==null&&notIn()) {
			Result result = Result.newSuccess();
			returnValue = result.msg(applicationContext.getMessage(Result.I18N_KEY_SUCCESS, null, CommonHelper.getLocale())).data(returnValue);
		}
		origin.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
	}

	private boolean notIn() {
		String requestURI = CommonHelper.getRequest().getRequestURI();
		for(String _uri:noWrapperUris) {
			String uri = _uri.trim();
			if(requestURI.startsWith(uri))
				return false;
		}
		return true;
	}
}