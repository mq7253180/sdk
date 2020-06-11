package com.quincy.auth.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.quincy.auth.o.DSession;

public interface AuthorizationCommonService {
	public DSession getSession(HttpServletRequest request) throws Exception;
	public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception;
	public void setExpiry(HttpServletRequest request) throws Exception;
}