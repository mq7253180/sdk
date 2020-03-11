package com.quincy.auth.service.impl;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import com.quincy.auth.AuthConstants;
import com.quincy.auth.o.DSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthCallback;
import com.quincy.core.InnerConstants;

@Service("authorizationSessionServiceImpl")
public class AuthorizationSessionServiceImpl extends AuthorizationAbstract {
	@Override
	protected Object getUserObject(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return session==null?null:session.getAttribute(InnerConstants.ATTR_SESSION);
	}

	@Override
	public DSession setSession(HttpServletRequest request, String originalJsessionid, Long userId, AuthCallback callback) {
		if(originalJsessionid!=null&&originalJsessionid.length()>0) {//同一user不同客户端登录互踢
			HttpSession httpSession = AuthConstants.SESSIONS.get(originalJsessionid);
			if(httpSession!=null) {
				DSession session = (DSession)httpSession.getAttribute(InnerConstants.ATTR_SESSION);
				if(session.getUser().getId().equals(userId))
					httpSession.invalidate();
			}
		}
		HttpSession httpSession = request.getSession();
		String jsessionid = httpSession.getId();
		User user = callback.getUser();
		user.setJsessionid(jsessionid);
		DSession session = this.createSession(user);
		httpSession.setAttribute(InnerConstants.ATTR_SESSION, session);
		callback.updateLastLogined(jsessionid);
		return session;
	}

	public void logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if(session!=null) {
			session.invalidate();
		}
	}

	@Override
	public void updateSession(User user) {
		HttpSession httpSession = AuthConstants.SESSIONS.get(user.getJsessionid());
		DSession dSession = this.createSession(user);
		httpSession.setAttribute(InnerConstants.ATTR_SESSION, dSession);
	}

	@Override
	public <T extends User> void updateSession(List<T> users) throws IOException {
		for(User user:users) {
			this.updateSession(user);
		}
	}
}