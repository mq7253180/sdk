package com.quincy.core.web;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.quincy.sdk.Client;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.HttpClientHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneralInterceptor extends HandlerInterceptorAdapter {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		log.warn(HttpClientHelper.getRequestURIOrURL(request, HttpClientHelper.FLAG_URL));
//		response.setHeader("Access-Control-Allow-Origin", domain);
//		response.setHeader("Access-Control-Allow-Credentials", "true");
//		response.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE");
//		response.setHeader("Access-Control-Max-Age", "3600");
//		response.setHeader("Access-Control-Allow-Headers", "accept, Accept, Origin, x-requested-with, XRequestedWith, XMLHttpRequest, Content-Type, contentType, LastModified, "+Constants.CLIENT_TOKEN);
//		Locale locale = CommonHelper.getLocale(request);
		/*
		 * 普通springmvc这样设置, spring-boot抛异常, spring-boot要通过实现LocaleResolver接口的bean实现
		 * java.lang.UnsupportedOperationException: Cannot change HTTP accept header - use a different locale resolution strategy
		 * 
		 * 这个设置主要是用在RequestContext requestContext = new RequestContext(request);requestContext.getMessage("key值")获取国际化msg的方式上, 在GlobalLocaleResolver上实现了
		 */
		/*LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
		localeResolver.setLocale(request, response, locale);*/
		/*
		 * 设置当前请求的整个线程的locale, 用在applicationContext.getMessage获取国际化msg方式上, 因为封装返回值时获取不到HttpServletRequest对象
		 */
//		Sync.getLocale().set(locale);
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
		if(modelAndView!=null) {
			String viewName = CommonHelper.trim(modelAndView.getViewName());
			if(viewName!=null&&!viewName.startsWith("redirect")&&!viewName.startsWith("forward")) {
				Client client = Client.get(request, handler);
				modelAndView.setViewName(modelAndView.getViewName()+"_"+client.getSuffix());
				if(client.getContentType()!=null)
					response.setHeader("Content-Type", client.getContentType());
			}
		}
	}
}