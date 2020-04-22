package com.quincy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthorizationCacheContext {
	@Value("${spring.application.name}")
	private String applicationName;

	@Bean("sessionKeyPrefix")
	public String sessionKeyPrefix() {
		return applicationName+".session.";
	}
}