package com.quincy.auth.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.AuthContext;
import com.quincy.auth.AuthHandler;
import com.quincy.auth.annotation.LoginRequired;

@Controller
@RequestMapping("")
public class RootController implements AuthContext {
	private AuthHandler authHandler;

	@LoginRequired
	@RequestMapping("")
	public ModelAndView root(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return authHandler==null?new ModelAndView("/index"):authHandler.indexView(request, response);
	}

	@GetMapping(value = "/static/**")
	public void handleStatic() {}

	@Override
	public void setAuthHandler(AuthHandler authHandler) {
		this.authHandler = authHandler;
	}
}