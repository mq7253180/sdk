package com.quincy.auth;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;

@Configuration
public class AuthClientContextConfiguration {
	@Bean
	public ProviderManager providerManager() {
		AuthenticationProvider provider = new OidcAuthorizationCodeAuthenticationProvider(null, null);
		List<AuthenticationProvider> providers = new ArrayList<AuthenticationProvider>();
		providers.add(provider);
		ProviderManager providerManager = new ProviderManager(providers);
		return providerManager;
	}
}