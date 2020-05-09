package com.quincy.core.web;

import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.quincy.core.ThreadLocalHolder;
import com.quincy.sdk.Result;
import com.quincy.sdk.helper.CommonHelper;

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
		result.setAccsessToken(ThreadLocalHolder.getAccsessToken());
		returnValue = result.msg(applicationContext.getMessage(Result.I18N_KEY_SUCCESS, null, CommonHelper.getLocale())).data(returnValue).cluster(cluster);
		origin.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
	}
}