package com.quincy.auth;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.springframework.context.annotation.Bean;

public class AuthSessionInitialization {
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
}