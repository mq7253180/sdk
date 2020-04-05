package com.quincy.auth.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.AuthConstants;
import com.quincy.auth.AuthContext;
import com.quincy.auth.AuthHandler;
import com.quincy.auth.annotation.LoginRequired;

@Controller
@RequestMapping("")
public class RootController implements AuthContext {
	private AuthHandler authHandler;

	@LoginRequired
	public ModelAndView root(HttpServletRequest request, HttpServletResponse response) {
		ModelAndView mv = null;
		if(authHandler!=null) {
			mv = authHandler.rootView(request, response);
			if(mv==null)
				mv = new ModelAndView("/index");
		}
		return mv;
	}

	@GetMapping(value = "/static/**")
	public void handleStatic() {}

	@Override
	public void setAuthHandler(AuthHandler authHandler) {
		this.authHandler = authHandler;
	}
	/**
	 * 进入密码设置页
	 */
	@LoginRequired
	@RequestMapping(AuthConstants.URI_PWD_SET)
	public String toPwdSet() {
		return "/password";
	}
}