package com.quincy.auth.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

@Controller
@RequestMapping("/oauth2")
public abstract class OAuth2ControllerSupport {
	protected abstract ModelAndView signinView(HttpServletRequest request);

	@RequestMapping("/code")
	public void accessCode() {
		
	}

	@RequestMapping("/token")
	public void accessToken() {
		
	}
	/**
	 * 进登录页
	 */
	@RequestMapping("/signin")
	public ModelAndView toLogin(HttpServletRequest request, @RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String _redirectTo) {
		ModelAndView mv = signinView(request);
		if(mv==null)
			mv = new ModelAndView("/login_oauth2");
		String redirectTo = CommonHelper.trim(_redirectTo);
		if(redirectTo!=null)
			mv.addObject(InnerConstants.PARAM_REDIRECT_TO, redirectTo);
		return mv;
	}
}