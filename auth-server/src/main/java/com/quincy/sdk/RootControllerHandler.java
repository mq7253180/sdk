package com.quincy.sdk;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

public interface RootControllerHandler {
	/**
	 * 设置了/路径ModelAndView设置定制化输入对象，用于在模板上引用
	 */
	public Map<String, ?> viewObjects(HttpServletRequest request) throws Exception;
	public boolean loginRequired();
}