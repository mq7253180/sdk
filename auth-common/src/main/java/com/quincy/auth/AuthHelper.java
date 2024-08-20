package com.quincy.auth;

import com.quincy.auth.o.XSession;
import com.quincy.sdk.helper.CommonHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class AuthHelper {
	public static XSession getSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		XSession xsession = session==null?null:(XSession)session.getAttribute(AuthConstants.ATTR_SESSION);
		return xsession;
	}

	public static XSession getSession() {
		HttpServletRequest request = CommonHelper.getRequest();
		return getSession(request);
	}

	public static void setSession(HttpServletRequest request, XSession xsession) {
		HttpSession session = request.getSession(false);
		session.setAttribute(AuthConstants.ATTR_SESSION, xsession);
	}

	public static void setSession(XSession xsession) {
		HttpServletRequest request = CommonHelper.getRequest();
		setSession(request, xsession);
	}
}