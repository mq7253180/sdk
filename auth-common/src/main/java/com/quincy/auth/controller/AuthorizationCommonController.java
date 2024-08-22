package com.quincy.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quincy.core.InnerConstants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
public class AuthorizationCommonController {
	/**
	 * 登出
	 */
	@RequestMapping("/signout")
	public String logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession(false);
		if(session!=null)
			session.invalidate();
		return InnerConstants.VIEW_PATH_SUCCESS;
	}
	/**
	 * 点超链接没权限要进入的页面
	 */
	@RequestMapping("/deny")
	public String deny() {
		return "/deny";
	}
}