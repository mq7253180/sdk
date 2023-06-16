package com.quincy.auth;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthServerCacheConfiguration implements BeanDefinitionRegistryPostProcessor {
	private final static String SERVICE_BEAN_NAME_TO_REMOVE = "authorizationServerServiceSessionImpl";

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		if (registry.containsBeanDefinition(SERVICE_BEAN_NAME_TO_REMOVE))
        	registry.removeBeanDefinition(SERVICE_BEAN_NAME_TO_REMOVE);
	}
}