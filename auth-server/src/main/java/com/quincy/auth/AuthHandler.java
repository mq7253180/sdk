package com.quincy.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

public interface AuthHandler {
	public ModelAndView rootView(HttpServletRequest request, HttpServletResponse response);
}