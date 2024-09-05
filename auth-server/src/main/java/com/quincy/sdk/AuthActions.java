package com.quincy.sdk;

import org.springframework.web.servlet.ModelAndView;

import com.quincy.sdk.o.User;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthActions {
	public abstract User findUser(String username, Client client);
	public abstract void updateLastLogin(Long userId, String jsessionid, Client client);
	public abstract void updatePassword(Long userId, String password);
	public abstract ModelAndView signinView(HttpServletRequest request);
}