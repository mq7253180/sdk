package com.quincy.core.web;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import com.quincy.sdk.helper.CommonHelper;

/**
 * 用在RequestContext requestContext = new RequestContext(request);requestContext.getMessage("key值")获取国际化msg的方式上
 */
public class GlobalLocaleResolver implements LocaleResolver {
	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		Locale locale = StringUtils.parseLocaleString(CommonHelper.getLocale(request));
		return locale;
	}

	@Override
	public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		/*LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
		localeResolver.setLocale(request, response, this.resolveLocale(request));*/
	}
}