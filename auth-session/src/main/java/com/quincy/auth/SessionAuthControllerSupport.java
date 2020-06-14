package com.quincy.auth;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.controller.AuthorizationControllerSupport;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.Result;

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