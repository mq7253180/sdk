package com.quincy.auth;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.quincy.auth.controller.RootController;
import com.quincy.auth.dao.PermissionRepository;
import com.quincy.auth.entity.Permission;
import com.quincy.sdk.RootControllerHandler;
import com.quincy.sdk.helper.RSASecurityHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class AuthServerInitialization {//implements BeanDefinitionRegistryPostProcessor {
	@Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
	@Autowired
	private RootController rootController;
	@Autowired(required = false)
	private RootControllerHandler rootControllerHandler;
	@Value("${secret.rsa.privateKey}")
	private String privateKeyStr;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		this.loadPermissions();
		if(rootControllerHandler!=null) {
			RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();
	        config.setPatternParser(requestMappingHandlerMapping.getPatternParser());
			RequestMappingInfo requestMappingInfo = RequestMappingInfo
					.paths("/")
	                .methods(RequestMethod.GET)
	                .options(config)
	                .build();
			requestMappingHandlerMapping.registerMapping(requestMappingInfo, rootController, RootController.class.getMethod(rootControllerHandler.loginRequired()?"rootWithLogin":"root", HttpServletRequest.class, HttpServletResponse.class));
		}
	}

	@Autowired
	private PermissionRepository permissionRepository;

	private void loadPermissions() {
		List<Permission> permissoins = permissionRepository.findAll();
		AuthConstants.PERMISSIONS = new HashMap<String, String>(permissoins.size());
		for(Permission permission:permissoins) {
			AuthConstants.PERMISSIONS.put(permission.getName(), permission.getDes());
		}
	}

	@Bean("selfPrivateKey")
	public PrivateKey privateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		return RSASecurityHelper.extractPrivateKey(privateKeyStr);
	}
	/*
	private final static String SERVICE_BEAN_NAME_TO_REMOVE = "authorizationServerServiceSessionImpl";

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		if (registry.containsBeanDefinition(SERVICE_BEAN_NAME_TO_REMOVE))
        	registry.removeBeanDefinition(SERVICE_BEAN_NAME_TO_REMOVE);
	}
	*/
}