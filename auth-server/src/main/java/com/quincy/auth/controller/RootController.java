package com.quincy.auth.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.AuthConstants;
import com.quincy.sdk.RootControllerHandler;
import com.quincy.sdk.annotation.auth.LoginRequired;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("")
public class RootController {
	@Autowired(required = false)
	private RootControllerHandler handler;

	public ModelAndView root(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView mv = new ModelAndView("/index").addObject(AuthConstants.ATTR_SESSION, request.getSession(false).getAttribute(AuthConstants.ATTR_SESSION));
		if(handler!=null) {
			Map<String, ?> map = handler.viewObjects(request);
			if(map!=null&&map.size()>0)
				mv.addAllObjects(map);
		}
		return mv;
	}

	@LoginRequired
	public ModelAndView rootWithLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.root(request, response);
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