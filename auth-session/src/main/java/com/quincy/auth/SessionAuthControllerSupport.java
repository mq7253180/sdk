package com.quincy.auth;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.controller.AuthorizationControllerSupport;
import com.quincy.sdk.Result;

public abstract class SessionAuthControllerSupport extends AuthorizationControllerSupport {
	/**
	 * 密码登录
	 */
	@PostMapping("/signin/pwd")
	public ModelAndView doLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = "username")String username, 
			@RequestParam(required = false, value = "password")String password, 
			@RequestParam(required = false, value = AuthConstants.PARAM_REDIRECT_TO)String redirectTo) throws Exception {
		Result result = doPwdLogin(request, username, password);
		ModelAndView mv = createModelAndView(request, result, redirectTo);
		return mv;
	}
}