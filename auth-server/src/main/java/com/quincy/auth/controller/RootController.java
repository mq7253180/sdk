package com.quincy.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.AuthContext;
import com.quincy.auth.AuthHandler;
import com.quincy.auth.AuthServerConstants;
import com.quincy.auth.annotation.LoginRequired;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("")
public class RootController implements AuthContext {
	private AuthHandler authHandler;

	@LoginRequired
	public ModelAndView root(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView mv = authHandler!=null?authHandler.rootView(request, response):new ModelAndView("/index");
		return mv;
	}

	@GetMapping("/static/**")
	public void handleStatic() {}

	@Override
	public void setAuthHandler(AuthHandler authHandler) {
		this.authHandler = authHandler;
	}
	/**
	 * 进入密码设置页
	 */
	@LoginRequired
	@RequestMapping(AuthServerConstants.URI_PWD_SET)
	public String toPwdSet() {
		return "/password";
	}
}