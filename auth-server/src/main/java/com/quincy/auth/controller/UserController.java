package com.quincy.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quincy.auth.annotation.LoginRequired;

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
	public void pwdUpdate(@RequestParam(required = true, name = "userid")Long userId, @RequestParam(required = true, name = "password")String password) {
		authActions.updatePassword(userId, password);
	}
}