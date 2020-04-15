package com.quincy.auth.interceptor;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.Assert;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.quincy.auth.annotation.LoginRequired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BearerTokenAuthenticationInterceptor extends HandlerInterceptorAdapter {
	private final AuthenticationManager authenticationManager;
	private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource =
			new WebAuthenticationDetailsSource();
	private BearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();
	private AuthenticationEntryPoint authenticationEntryPoint = new BearerTokenAuthenticationEntryPoint();

	public BearerTokenAuthenticationInterceptor(AuthenticationManager authenticationManager) {
		Assert.notNull(authenticationManager, "authenticationManager cannot be null");
		this.authenticationManager = authenticationManager;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException, ServletException {
		if(handler instanceof HandlerMethod) {
			boolean pass = false;
			HandlerMethod method = (HandlerMethod)handler;
			boolean loginRequired = method.getMethod().getDeclaredAnnotation(LoginRequired.class)!=null;
			if(loginRequired) {
				final boolean debug = log.isDebugEnabled();
				String token = null;
				try {
					token = this.bearerTokenResolver.resolve(request);
				} catch ( OAuth2AuthenticationException invalid ) {
					this.authenticationEntryPoint.commence(request, response, invalid);
				}
				if(token == null) {
//					filterChain.doFilter(request, response);
					pass = true;
				}
				BearerTokenAuthenticationToken authenticationRequest = new BearerTokenAuthenticationToken(token);
				authenticationRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
				try {
					Authentication authenticationResult = this.authenticationManager.authenticate(authenticationRequest);
					SecurityContext context = SecurityContextHolder.createEmptyContext();
					context.setAuthentication(authenticationResult);
					SecurityContextHolder.setContext(context);
//					filterChain.doFilter(request, response);
					pass = true;
				} catch (AuthenticationException failed) {
					SecurityContextHolder.clearContext();
					if(debug) {
						log.debug("Authentication request for failed: " + failed);
					}
					this.authenticationEntryPoint.commence(request, response, failed);
				}
			}
			return pass;
		} else
			return true;
	}

	public final void setBearerTokenResolver(BearerTokenResolver bearerTokenResolver) {
		Assert.notNull(bearerTokenResolver, "bearerTokenResolver cannot be null");
		this.bearerTokenResolver = bearerTokenResolver;
	}

	public final void setAuthenticationEntryPoint(final AuthenticationEntryPoint authenticationEntryPoint) {
		Assert.notNull(authenticationEntryPoint, "authenticationEntryPoint cannot be null");
		this.authenticationEntryPoint = authenticationEntryPoint;
	}
}