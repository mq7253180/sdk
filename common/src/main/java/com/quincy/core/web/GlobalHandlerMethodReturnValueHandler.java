package com.quincy.core.web;

import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.quincy.core.Sync;
import com.quincy.sdk.Result;

public class GlobalHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {
	private HandlerMethodReturnValueHandler origin;
	private ApplicationContext applicationContext;
	private String cluster;

	public GlobalHandlerMethodReturnValueHandler(HandlerMethodReturnValueHandler origin, ApplicationContext applicationContext, String cluster) {
		this.origin = origin;
		this.applicationContext = applicationContext;
		this.cluster = cluster;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return origin.supportsReturnType(returnType);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws Exception {
		Result result = Result.newSuccess();
		returnValue = result.msg(applicationContext.getMessage(Result.I18N_KEY_SUCCESS, null, Sync.getLocaleThreadLocal().get())).data(returnValue).cluster(cluster);
		origin.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
	}
}
