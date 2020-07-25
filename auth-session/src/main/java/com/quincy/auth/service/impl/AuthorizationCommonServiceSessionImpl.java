package com.quincy.auth.service.impl;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import com.quincy.core.InnerConstants;

@Service
public class AuthorizationCommonServiceSessionImpl extends AuthorizationCommonServiceSupport {
	@Override
	protected Object getUserObject(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return session==null?null:session.getAttribute(InnerConstants.ATTR_SESSION);
	}

	public void logout(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if(session!=null) {
			session.invalidate();
		}
	}

	@Override
	public void setExpiry(HttpServletRequest request, boolean deleteCookieIfExpired) throws Exception {}
}