package com.quincy.auth;

import javax.servlet.http.HttpServletRequest;

import com.quincy.auth.o.XSession;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

public class AuthHelper {
	public static XSession getSession(HttpServletRequest request) {
		XSession session = (XSession)request.getAttribute(InnerConstants.ATTR_SESSION);
		return session;
	}

	public static XSession getSession() {
		HttpServletRequest request = CommonHelper.getRequest();
		return getSession(request);
	}
}