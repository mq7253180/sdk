package com.quincy.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quincy.auth.annotation.LoginRequired;

@Controller
@RequestMapping("/user")
public class UserController {
	@LoginRequired
	@RequestMapping("/pwdset")
	public String pwdsetPage() {
		return "/password";
	}
}