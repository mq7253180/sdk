package com.quincy.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quincy.sdk.AuthActions;
import com.quincy.sdk.AuthHelper;
import com.quincy.sdk.annotation.auth.LoginRequired;
import com.quincy.sdk.o.User;
import com.quincy.sdk.o.XSession;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private AuthActions authActions;

	@LoginRequired
	@RequestMapping("/pwdset")
	public String pwdsetPage() {
		return "/password";
	}

	@LoginRequired
	@RequestMapping("/pwdset/update")
	public void pwdUpdate(HttpServletRequest request, @RequestParam(required = true, name = "password")String password) {
		XSession xsession = AuthHelper.getSession(request);
		User userPo = xsession.getUser();
		authActions.updatePassword(userPo.getId(), password);
	}
}