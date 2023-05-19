package com.quincy.auth;

import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthHandler {
	public ModelAndView rootView(HttpServletRequest request, HttpServletResponse response) throws Exception;
}