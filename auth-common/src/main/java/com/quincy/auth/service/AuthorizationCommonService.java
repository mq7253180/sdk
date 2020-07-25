package com.quincy.auth.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.quincy.auth.o.XSession;

public interface AuthorizationCommonService {
	public XSession getSession(HttpServletRequest request) throws Exception;
	public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception;
	public void setExpiry(HttpServletRequest request, boolean deleteCookieIfExpired) throws Exception;
}