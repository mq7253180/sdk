package com.quincy.auth.service.impl;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.quincy.auth.AuthSessionHolder;
import com.quincy.auth.o.XSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthCallback;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class AuthorizationServerServiceSessionImpl extends AuthorizationServerServiceSupport {
	@Override
	public XSession setSession(HttpServletRequest request, String originalJsessionid, Long userId, AuthCallback callback) {
		if(originalJsessionid!=null) {//同一user不同客户端登录互踢
			HttpSession httpSession = AuthSessionHolder.SESSIONS.get(originalJsessionid);
			if(httpSession!=null) {
				XSession session = (XSession)httpSession.getAttribute(InnerConstants.ATTR_SESSION);
				if(session.getUser().getId().equals(userId))
					httpSession.invalidate();
			}
		}
		HttpSession httpSession = request.getSession();
		String jsessionid = httpSession.getId();
		User user = callback.getUser();
		user.setJsessionid(jsessionid);
		XSession session = this.createSession(user);
		httpSession.setAttribute(InnerConstants.ATTR_SESSION, session);
		callback.updateLastLogined(jsessionid);
		return session;
	}

	@Override
	public void updateSession(User user) {
		String jsessionid = CommonHelper.trim(user.getJsessionid());
		if(jsessionid!=null) {
			HttpSession httpSession = AuthSessionHolder.SESSIONS.get(jsessionid);
			XSession session = this.createSession(user);
			httpSession.setAttribute(InnerConstants.ATTR_SESSION, session);
		}
	}

	@Override
	public void updateSession(List<User> users) throws IOException {
		for(User user:users) {
			this.updateSession(user);
		}
	}
}