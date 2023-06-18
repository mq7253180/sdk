package com.quincy.auth.controller;

import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.o.User;
import com.quincy.sdk.Client;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthActions {
	public abstract User findUser(String username, Client client);
	public abstract void updateLastLogin(Long userId, Client client, String jsessionid);
	public abstract ModelAndView signinView(HttpServletRequest request);
}