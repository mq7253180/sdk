package com.quincy.auth;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.quincy.auth.controller.RootController;
import com.quincy.auth.dao.PermissionRepository;
import com.quincy.auth.entity.Permission;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.RSASecurityHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class AuthServerInitialization {
	@Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
	@Autowired
	private RootController rootController;
	@Autowired
	@Qualifier(InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;
	@Value("${secret.rsa.privateKey}")
	private String privateKeyStr;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		this.loadPermissions();
		String _loginRequiredForRoot = CommonHelper.trim(properties.getProperty("auth.loginRequired.root"));
		boolean loginRequiredForRoot = _loginRequiredForRoot==null?false:Boolean.parseBoolean(_loginRequiredForRoot);
		if(loginRequiredForRoot) {
			RequestMappingInfo requestMappingInfo = RequestMappingInfo
					.paths("")
	                .methods(RequestMethod.GET)
	                .build();
			requestMappingHandlerMapping.registerMapping(requestMappingInfo, rootController, rootController.getClass().getMethod("root", HttpServletRequest.class, HttpServletResponse.class));
		}
	}

	@PreDestroy
	private void destroy() {
		
	}

	@Autowired
	private PermissionRepository permissionRepository;

	private void loadPermissions() {
		List<Permission> permissoins = permissionRepository.findAll();
		AuthCommonConstants.PERMISSIONS = new HashMap<String, String>(permissoins.size());
		for(Permission permission:permissoins) {
			AuthCommonConstants.PERMISSIONS.put(permission.getName(), permission.getDes());
		}
	}

	@Bean("selfPrivateKey")
	public PrivateKey privateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		return RSASecurityHelper.extractPrivateKey(privateKeyStr);
	}
}