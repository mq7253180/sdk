package com.quincy.sdk;

import com.quincy.auth.AuthConstants;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.o.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class AuthHelper {
	public static User getUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		User user = session==null?null:(User)session.getAttribute(AuthConstants.ATTR_SESSION);
		return user;
	}

	public static User getUser() {
		HttpServletRequest request = CommonHelper.getRequest();
		return getUser(request);
	}

	public static void setUser(HttpServletRequest request, User user) {
		request.getSession(false).setAttribute(AuthConstants.ATTR_SESSION, user);
	}

	public static Object getUserExt(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		Object userExt = session==null?null:session.getAttribute(AuthConstants.ATTR_USER_EXT);
		return userExt;
	}

	public static Object getUserExt() {
		HttpServletRequest request = CommonHelper.getRequest();
		return getUserExt(request);
	}
}