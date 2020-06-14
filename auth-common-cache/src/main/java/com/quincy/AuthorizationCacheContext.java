package com.quincy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthorizationCacheContext {
	@Value("${auth.name}")
	private String authName;

	@Bean("sessionKeyPrefix")
	public String sessionKeyPrefix() {
		return "session."+authName+".";
	}
}