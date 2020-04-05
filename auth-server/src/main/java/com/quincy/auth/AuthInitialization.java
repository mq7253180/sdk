package com.quincy.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.quincy.auth.controller.RootController;
import com.quincy.auth.dao.PermissionRepository;
import com.quincy.auth.entity.Permission;
import com.quincy.auth.freemarker.ButtonTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.DivTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.HyperlinkTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.InputTemplateDirectiveModelBean;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

@Configuration
public class AuthInitialization {
	@Autowired
    private freemarker.template.Configuration configuration;
	@Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
	@Autowired
	private RootController rootController;
	@Resource(name = InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		configuration.setSharedVariable("input", new InputTemplateDirectiveModelBean());
    	configuration.setSharedVariable("a", new HyperlinkTemplateDirectiveModelBean());
    	configuration.setSharedVariable("button", new ButtonTemplateDirectiveModelBean());
    	configuration.setSharedVariable("div", new DivTemplateDirectiveModelBean());
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
		AuthConstants.PERMISSIONS = new HashMap<String, String>(permissoins.size());
		for(Permission permission:permissoins) {
			AuthConstants.PERMISSIONS.put(permission.getName(), permission.getDes());
		}
	}

	@Bean
    public HttpSessionListener httpSessionListener() {
		return new HttpSessionListener() {
			@Override
			public void sessionCreated(HttpSessionEvent hse) {
				AuthConstants.SESSIONS.put(hse.getSession().getId(), hse.getSession());
			}

			@Override
			public void sessionDestroyed(HttpSessionEvent hse) {
				AuthConstants.SESSIONS.remove(hse.getSession().getId());
			}
		};
	}
}
