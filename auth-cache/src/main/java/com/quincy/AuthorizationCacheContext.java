package com.quincy;

import java.util.Properties;
import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.sdk.Constants;

@Configuration
public class AuthorizationCacheContext {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@Bean("sessionKeyPrefix")
	public String sessionKeyPrefix() {
		return properties.getProperty("spring.application.name")+".session.";
	}
}