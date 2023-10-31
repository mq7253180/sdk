package com.quincy.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZookeeperInitFirst {
	@Value("${spring.application.name}")
	private String applicationName;

	@Bean("zookeeperRootNode")
	public String zookeeperRootNode() {
		return "/"+applicationName;
	}
}