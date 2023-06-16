package com.quincy.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.AuthCommonConstants;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.Result;

import jakarta.servlet.http.HttpServletRequest;

public abstract class SessionAuthControllerSupport extends AuthorizationControllerSupport {
	/**
	 * 密码登录
	 */
	@PostMapping("/signin/pwd")
	public ModelAndView doLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = AuthCommonConstants.PARA_NAME_USERNAME)String username, 
			@RequestParam(required = false, value = AuthCommonConstants.PARA_NAME_PASSWORD)String password, 
			@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo) throws Exception {
		Result result = doPwdLogin(request, username, password);
		ModelAndView mv = createModelAndView(request, result, redirectTo);
		return mv;
	}
}