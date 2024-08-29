package com.quincy.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quincy.auth.AuthHelper;
import com.quincy.auth.annotation.LoginRequired;
import com.quincy.auth.o.User;
import com.quincy.auth.o.XSession;

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
		User userVo = new User();
		userVo.setId(userPo.getId());
		userVo.setPassword(password);
		authActions.updatePassword(userVo);
	}
}