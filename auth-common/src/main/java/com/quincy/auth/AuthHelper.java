package com.quincy.auth;

import javax.servlet.http.HttpServletRequest;

import com.quincy.auth.o.DSession;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

public class AuthHelper {
	public static DSession getSession(HttpServletRequest request) {
		DSession session = (DSession)request.getAttribute(InnerConstants.ATTR_SESSION);
		return session;
	}

	public static DSession getSession() {
		HttpServletRequest request = CommonHelper.getRequest();
		return getSession(request);
	}
}