package com.quincy.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;

import com.quincy.sdk.annotation.auth.EnableOriginalSessionEviction;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

public class SessionDestroyedOriginalConfiguration extends BaseSessionDestroyedConfiguration {
	public final static Map<String, HttpSession> SESSIONS = new ConcurrentHashMap<String, HttpSession>(1024);

	@Bean
    public HttpSessionListener httpSessionListener() {
		return new HttpSessionListener() {
			@Override
			public void sessionCreated(HttpSessionEvent hse) {
				SESSIONS.put(hse.getSession().getId(), hse.getSession());
			}

			@Override
			public void sessionDestroyed(HttpSessionEvent hse) {
				SESSIONS.remove(hse.getSession().getId());
			}
		};
	}

	@Override
	public void invalidate(String jsessionid) {
		HttpSession httpSession = SESSIONS.remove(jsessionid);
		if(httpSession!=null)
			httpSession.invalidate();
	}

	@Override
	protected Class<?> annotationClass() {
		return EnableOriginalSessionEviction.class;
	}
}