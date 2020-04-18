package com.quincy.auth.service.impl;

import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.OAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quincy.auth.dao.ClientSystemRepository;
import com.quincy.auth.entity.ClientSystem;
import com.quincy.auth.service.OAuth2Service;
import com.quincy.sdk.annotation.ReadOnly;

@Service
public class OAuth2ServiceImpl implements OAuth2Service {
	@Autowired
	private ClientSystemRepository clientSystemRepository;

	@ReadOnly
	@Override
	public ClientSystem findClientSystem(String clientId) {
		return clientSystemRepository.findByClientId(clientId);
	}

	@ReadOnly
	@Override
	public ClientSystem findClientSystem(Long id) {
		Optional<ClientSystem> o = clientSystemRepository.findById(id);
		return o.isPresent()?o.get():null;
	}

	public static void main(String[] args) {
		System.out.println("SC_ACCEPTED------------------"+HttpServletResponse.SC_ACCEPTED);
		System.out.println("SC_BAD_GATEWAY------------------"+HttpServletResponse.SC_BAD_GATEWAY);
		System.out.println("SC_BAD_REQUEST------------------"+HttpServletResponse.SC_BAD_REQUEST);
		System.out.println("SC_CONFLICT------------------"+HttpServletResponse.SC_CONFLICT);
		System.out.println("SC_CONTINUE------------------"+HttpServletResponse.SC_CONTINUE);
		System.out.println("SC_CREATED------------------"+HttpServletResponse.SC_CREATED);
		System.out.println("SC_EXPECTATION_FAILED------------------"+HttpServletResponse.SC_EXPECTATION_FAILED);
		System.out.println("SC_FORBIDDEN------------------"+HttpServletResponse.SC_FORBIDDEN);
		System.out.println("SC_FOUND------------------"+HttpServletResponse.SC_FOUND);
		System.out.println("SC_GATEWAY_TIMEOUT------------------"+HttpServletResponse.SC_GATEWAY_TIMEOUT);
		System.out.println("SC_GONE------------------"+HttpServletResponse.SC_GONE);
		System.out.println("SC_HTTP_VERSION_NOT_SUPPORTED------------------"+HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED);
		System.out.println("SC_INTERNAL_SERVER_ERROR------------------"+HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		System.out.println("SC_LENGTH_REQUIRED------------------"+HttpServletResponse.SC_LENGTH_REQUIRED);
		System.out.println("SC_METHOD_NOT_ALLOWED------------------"+HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		System.out.println("SC_MOVED_PERMANENTLY------------------"+HttpServletResponse.SC_MOVED_PERMANENTLY);
		System.out.println("SC_MOVED_TEMPORARILY------------------"+HttpServletResponse.SC_MOVED_TEMPORARILY);
		System.out.println("SC_MULTIPLE_CHOICES------------------"+HttpServletResponse.SC_MULTIPLE_CHOICES);
		System.out.println("SC_NO_CONTENT------------------"+HttpServletResponse.SC_NO_CONTENT);
		System.out.println("SC_NON_AUTHORITATIVE_INFORMATION------------------"+HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION);
		System.out.println("SC_NOT_ACCEPTABLE------------------"+HttpServletResponse.SC_NOT_ACCEPTABLE);
		System.out.println("SC_NOT_FOUND------------------"+HttpServletResponse.SC_NOT_FOUND);
		System.out.println("SC_NOT_IMPLEMENTED------------------"+HttpServletResponse.SC_NOT_IMPLEMENTED);
		System.out.println("SC_NOT_MODIFIED------------------"+HttpServletResponse.SC_NOT_MODIFIED);
		System.out.println("SC_OK------------------"+HttpServletResponse.SC_OK);
		System.out.println("SC_PARTIAL_CONTENT------------------"+HttpServletResponse.SC_PARTIAL_CONTENT);
		System.out.println("SC_PAYMENT_REQUIRED------------------"+HttpServletResponse.SC_PAYMENT_REQUIRED);
		System.out.println("SC_PRECONDITION_FAILED------------------"+HttpServletResponse.SC_PRECONDITION_FAILED);
		System.out.println("SC_PROXY_AUTHENTICATION_REQUIRED------------------"+HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
		System.out.println("SC_REQUEST_ENTITY_TOO_LARGE------------------"+HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
		System.out.println("SC_REQUEST_TIMEOUT------------------"+HttpServletResponse.SC_REQUEST_TIMEOUT);
		System.out.println("SC_REQUEST_URI_TOO_LONG------------------"+HttpServletResponse.SC_REQUEST_URI_TOO_LONG);
		System.out.println("SC_REQUESTED_RANGE_NOT_SATISFIABLE------------------"+HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
		System.out.println("SC_RESET_CONTENT------------------"+HttpServletResponse.SC_RESET_CONTENT);
		System.out.println("SC_SEE_OTHER------------------"+HttpServletResponse.SC_SEE_OTHER);
		System.out.println("SC_SERVICE_UNAVAILABLE------------------"+HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		System.out.println("SC_SWITCHING_PROTOCOLS------------------"+HttpServletResponse.SC_SWITCHING_PROTOCOLS);
		System.out.println("SC_TEMPORARY_REDIRECT------------------"+HttpServletResponse.SC_TEMPORARY_REDIRECT);
		System.out.println("SC_UNAUTHORIZED------------------"+HttpServletResponse.SC_UNAUTHORIZED);
		System.out.println("SC_UNSUPPORTED_MEDIA_TYPE------------------"+HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		System.out.println("SC_USE_PROXY------------------"+HttpServletResponse.SC_USE_PROXY);
		System.out.println("====================================");
		System.out.println("------------------"+OAuth.ASSERTION);
		System.out.println("------------------"+OAuth.OAUTH_ACCESS_TOKEN);
		System.out.println("------------------"+OAuth.OAUTH_ASSERTION);
		System.out.println("------------------"+OAuth.OAUTH_ASSERTION_TYPE);
		System.out.println("------------------"+OAuth.OAUTH_BEARER_TOKEN);
		System.out.println("------------------"+OAuth.OAUTH_CLIENT_ID);
		System.out.println("------------------"+OAuth.OAUTH_CLIENT_SECRET);
		System.out.println("------------------"+OAuth.OAUTH_CODE);
		System.out.println("------------------"+OAuth.OAUTH_EXPIRES_IN);
		System.out.println("------------------"+OAuth.OAUTH_GRANT_TYPE);
		System.out.println("------------------"+OAuth.OAUTH_HEADER_NAME);
		System.out.println("------------------"+OAuth.OAUTH_PASSWORD);
		System.out.println("------------------"+OAuth.OAUTH_REDIRECT_URI);
		System.out.println("------------------"+OAuth.OAUTH_REFRESH_TOKEN);
		System.out.println("------------------"+OAuth.OAUTH_RESPONSE_TYPE);
		System.out.println("------------------"+OAuth.OAUTH_SCOPE);
		System.out.println("------------------"+OAuth.OAUTH_STATE);
		System.out.println("------------------"+OAuth.OAUTH_TOKEN);
		System.out.println("------------------"+OAuth.OAUTH_TOKEN_DRAFT_0);
		System.out.println("------------------"+OAuth.OAUTH_TOKEN_TYPE);
		System.out.println("------------------"+OAuth.OAUTH_USERNAME);
		System.out.println("------------------"+OAuth.OAUTH_VERSION_DIFFER);
		System.out.println("------------------"+OAuth.ContentType.JSON);
		System.out.println("------------------"+OAuth.ContentType.URL_ENCODED);
		System.out.println("------------------"+OAuth.HeaderType.AUTHORIZATION);
		System.out.println("------------------"+OAuth.HeaderType.CONTENT_TYPE);
		System.out.println("------------------"+OAuth.HeaderType.WWW_AUTHENTICATE);
		System.out.println("------------------"+OAuth.WWWAuthHeader.REALM);
	}
}