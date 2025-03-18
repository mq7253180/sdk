package com.quincy.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quincy.auth.service.UserService;
import com.quincy.sdk.AuthHelper;
import com.quincy.sdk.annotation.auth.LoginRequired;
import com.quincy.sdk.o.User;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private UserService userService;

	@LoginRequired
	@RequestMapping("/pwdset")
	public String pwdsetPage() {
		return "/password";
	}

	@LoginRequired
	@RequestMapping("/pwdset/update")
	public void pwdUpdate(HttpServletRequest request, @RequestParam(required = true, name = "password")String password) {
		User user = AuthHelper.getUser(request);
		userService.updatePassword(user.getId(), password);
	}
}