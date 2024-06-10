package com.quincy.auth;

import org.springframework.context.annotation.Bean;

import com.quincy.core.SessionInvalidation;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

public class SessionDestroyedOriginalConfiguration implements SessionInvalidation {
	@Bean
    public HttpSessionListener httpSessionListener() {
		return new HttpSessionListener() {
			@Override
			public void sessionCreated(HttpSessionEvent hse) {
				AuthSessionHolder.SESSIONS.put(hse.getSession().getId(), hse.getSession());
			}

			@Override
			public void sessionDestroyed(HttpSessionEvent hse) {
				AuthSessionHolder.SESSIONS.remove(hse.getSession().getId());
			}
		};
	}

	@Override
	public void invalidate(String jsessionid) {
		HttpSession httpSession = AuthSessionHolder.SESSIONS.remove(jsessionid);
		if(httpSession!=null)
			httpSession.invalidate();
	}
}