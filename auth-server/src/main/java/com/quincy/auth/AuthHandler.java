package com.quincy.auth;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthHandler {
	/**
	 * 如果auth.loginRequired.root=true设置了/需要登录，用于给ModelAndView设置定制化输入对象，用于在模板上引用
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public Map<String, ?> rootViewObjects(HttpServletRequest request) throws Exception;
}