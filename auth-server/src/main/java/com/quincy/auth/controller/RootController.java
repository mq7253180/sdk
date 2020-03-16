package com.quincy.auth.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.AuthConstants;
import com.quincy.auth.AuthContext;
import com.quincy.auth.AuthHandler;
import com.quincy.auth.annotation.LoginRequired;
import com.quincy.auth.o.DSession;
import com.quincy.auth.service.AuthorizationService;

@Controller
@RequestMapping("")
public class RootController implements AuthContext {
	@Autowired
	private AuthorizationService authorizationService;
	private AuthHandler authHandler;

	@RequestMapping("")
	public String root(HttpServletRequest request) throws Exception {
		DSession session = authorizationService.getSession(request);
		String uri = session==null?"/auth/signin":AuthConstants.URI_INDEX;
		return "redirect:"+uri;
	}

	@LoginRequired
	@GetMapping(value = AuthConstants.URI_INDEX)
	public ModelAndView index(HttpServletRequest request, HttpServletResponse response) {
		return authHandler==null?new ModelAndView(AuthConstants.URI_INDEX):authHandler.indexView(request, response);
	}

	@GetMapping(value = "/static/**")
	public void handleStatic() {}

	@Override
	public void setAuthHandler(AuthHandler authHandler) {
		this.authHandler = authHandler;
	}
}