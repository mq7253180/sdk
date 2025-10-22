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
	private boolean noInnerWrapper = false;

	public GlobalHandlerMethodReturnValueHandler(HandlerMethodReturnValueHandler origin, ApplicationContext applicationContext, String[] noWrapperUris, boolean noInnerWrapper) {
		this.origin = origin;
		this.applicationContext = applicationContext;
		this.noWrapperUris = noWrapperUris;
		this.noInnerWrapper = noInnerWrapper;
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
			Result result = null;
			if(noInnerWrapper&&returnValue instanceof Result) {
				result = (Result)returnValue;
			} else {
				result = Result.newSuccess();
				returnValue = result.msg(applicationContext.getMessage(Result.I18N_KEY_SUCCESS, null, CommonHelper.getLocale())).data(returnValue);
			}
		}
		origin.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
	}

	private boolean notIn() {
		if(noWrapperUris!=null&&noWrapperUris.length>0) {
			String requestURI = CommonHelper.getRequest().getRequestURI();
			for(String _uri:noWrapperUris) {
				String uri = _uri.trim();
				if(uri.length()>0&&requestURI.startsWith(uri))
					return false;
			}
		}
		return true;
	}
}