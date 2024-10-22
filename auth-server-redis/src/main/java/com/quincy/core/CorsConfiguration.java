package com.quincy.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@Order(SessionRepositoryFilter.DEFAULT_ORDER-1)
//@Configuration
public class CorsConfiguration extends OncePerRequestFilter {
	@Autowired
	private DefaultCookieSerializer defaultCookieSerializer;
	@Autowired
	private SpringHttpSessionConfiguration springHttpSessionConfiguration;

	@PostConstruct
	public void init() {
		defaultCookieSerializer.setSameSite("None");
		List<HttpSessionListener> listeners = new ArrayList<>();
		listeners.add(new HttpSessionListener() {
			@Override
			public void sessionCreated(HttpSessionEvent hse) {
				System.out.println("==============sessionCreated");
			}

			@Override
			public void sessionDestroyed(HttpSessionEvent hse) {
				System.out.println("==============sessionDestroyed");
			}
		});
		springHttpSessionConfiguration.setHttpSessionListeners(listeners);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
//		System.out.println("BEFORE=============="+response.getHeader("Set-Cookie")+"---"+request.getClass().getName());
		filterChain.doFilter(new SecureTrueRequestWrapper(request), response);
		String setCookie = response.getHeader("Set-Cookie");
		if(setCookie!=null) {
			setCookie += "; Partitioned";
			response.setHeader("Set-Cookie", setCookie);
//			System.out.println("AFTER==============\r\n"+response.getHeader("Set-Cookie")+"\r\n"+setCookie);
		}
	}
}