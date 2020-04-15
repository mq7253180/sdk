package com.quincy.auth.interceptor;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.UriComponentsBuilder;

import com.quincy.auth.annotation.LoginRequired;

public class OAuth2AuthorizationCodeGrantInterceptor extends HandlerInterceptorAdapter {
	private final ClientRegistrationRepository clientRegistrationRepository;
	private final OAuth2AuthorizedClientRepository authorizedClientRepository;
	private final AuthenticationManager authenticationManager;
	private AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository =
		new HttpSessionOAuth2AuthorizationRequestRepository();
	private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private final RequestCache requestCache = new HttpSessionRequestCache();

	public OAuth2AuthorizationCodeGrantInterceptor(ClientRegistrationRepository clientRegistrationRepository,
			OAuth2AuthorizedClientRepository authorizedClientRepository,
			AuthenticationManager authenticationManager) {
		Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
		Assert.notNull(authorizedClientRepository, "authorizedClientRepository cannot be null");
		Assert.notNull(authenticationManager, "authenticationManager cannot be null");
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.authorizedClientRepository = authorizedClientRepository;
		this.authenticationManager = authenticationManager;
	}

	public final void setAuthorizationRequestRepository(AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository) {
		Assert.notNull(authorizationRequestRepository, "authorizationRequestRepository cannot be null");
		this.authorizationRequestRepository = authorizationRequestRepository;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws ServletException, IOException {
		if(handler instanceof HandlerMethod) {
			boolean pass = false;
			HandlerMethod method = (HandlerMethod)handler;
			boolean loginRequired = method.getMethod().getDeclaredAnnotation(LoginRequired.class)!=null;
			if(loginRequired) {
				if(this.shouldProcessAuthorizationResponse(request)) {
					this.processAuthorizationResponse(request, response);
				} else
					pass = true;
			}
			return pass;
		} else
			return true;
	}

	private boolean shouldProcessAuthorizationResponse(HttpServletRequest request) {
		OAuth2AuthorizationRequest authorizationRequest = this.authorizationRequestRepository.loadAuthorizationRequest(request);
		if (authorizationRequest == null) {
			return false;
		}
		String requestUrl = UrlUtils.buildFullRequestUrl(request.getScheme(), request.getServerName(),
				request.getServerPort(), request.getRequestURI(), null);
		MultiValueMap<String, String> params = OAuth2AuthorizationResponseUtils.toMultiMap(request.getParameterMap());
		if (requestUrl.equals(authorizationRequest.getRedirectUri()) &&
				OAuth2AuthorizationResponseUtils.isAuthorizationResponse(params)) {
			return true;
		}
		return false;
	}

	private void processAuthorizationResponse(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		OAuth2AuthorizationRequest authorizationRequest =
				this.authorizationRequestRepository.removeAuthorizationRequest(request, response);

		String registrationId = (String) authorizationRequest.getAdditionalParameters().get(OAuth2ParameterNames.REGISTRATION_ID);
		ClientRegistration clientRegistration = this.clientRegistrationRepository.findByRegistrationId(registrationId);

		MultiValueMap<String, String> params = OAuth2AuthorizationResponseUtils.toMultiMap(request.getParameterMap());
		String redirectUri = UriComponentsBuilder.fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
				.replaceQuery(null)
				.build()
				.toUriString();
		OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponseUtils.convert(params, redirectUri);

		OAuth2AuthorizationCodeAuthenticationToken authenticationRequest = new OAuth2AuthorizationCodeAuthenticationToken(
			clientRegistration, new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse));
		authenticationRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));

		OAuth2AuthorizationCodeAuthenticationToken authenticationResult;

		try {
			authenticationResult = (OAuth2AuthorizationCodeAuthenticationToken)
				this.authenticationManager.authenticate(authenticationRequest);
		} catch (OAuth2AuthorizationException ex) {
			OAuth2Error error = ex.getError();
			UriComponentsBuilder uriBuilder = UriComponentsBuilder
				.fromUriString(authorizationResponse.getRedirectUri())
				.queryParam(OAuth2ParameterNames.ERROR, error.getErrorCode());
			if (!StringUtils.isEmpty(error.getDescription())) {
				uriBuilder.queryParam(OAuth2ParameterNames.ERROR_DESCRIPTION, error.getDescription());
			}
			if (!StringUtils.isEmpty(error.getUri())) {
				uriBuilder.queryParam(OAuth2ParameterNames.ERROR_URI, error.getUri());
			}
			this.redirectStrategy.sendRedirect(request, response, uriBuilder.build().encode().toString());
			return;
		}

		Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
		String principalName = currentAuthentication != null ? currentAuthentication.getName() : "anonymousUser";

		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
			authenticationResult.getClientRegistration(),
			principalName,
			authenticationResult.getAccessToken(),
			authenticationResult.getRefreshToken());

		this.authorizedClientRepository.saveAuthorizedClient(authorizedClient, currentAuthentication, request, response);

		String redirectUrl = authorizationResponse.getRedirectUri();
		SavedRequest savedRequest = this.requestCache.getRequest(request, response);
		if (savedRequest != null) {
			redirectUrl = savedRequest.getRedirectUrl();
			this.requestCache.removeRequest(request, response);
		}

		this.redirectStrategy.sendRedirect(request, response, redirectUrl);
	}
}