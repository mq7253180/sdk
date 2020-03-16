package com.quincy.auth;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.controller.AuthorizationControllerSupport;
import com.quincy.sdk.Result;
import com.quincy.sdk.annotation.VCodeRequired;

public abstract class VCodeAuthControllerSupport extends AuthorizationControllerSupport {
	/**
	 * 验证码登录
	 */
	@VCodeRequired
	@PostMapping("/signin/vcode")
	public ModelAndView doLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = "username")String _username, 
			@RequestParam(required = false, value = AuthConstants.PARAM_BACK_TO)String _backTo) throws Exception {
		Result result = this.login(request, _username, null);
		return this.createModelAndView(request, result, _backTo);
	}
}