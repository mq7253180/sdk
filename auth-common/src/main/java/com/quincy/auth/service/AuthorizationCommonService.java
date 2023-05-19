package com.quincy.auth.service;

import com.quincy.auth.o.XSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthorizationCommonService {
	public XSession getSession(HttpServletRequest request) throws Exception;
	public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception;
	public void setExpiry(HttpServletRequest request, boolean deleteCookieIfExpired) throws Exception;
}