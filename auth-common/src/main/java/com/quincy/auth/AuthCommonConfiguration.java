package com.quincy.auth;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.quincy.auth.freemarker.ButtonTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.DivTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.HyperlinkTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.InputTemplateDirectiveModelBean;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class AuthCommonConfiguration {
	@Autowired
    private freemarker.template.Configuration configuration;
	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;
	@Autowired(required = false)
	private MultiEnterpriseConfiguration multiEnterpriseConfiguration;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		configuration.setSharedVariable("input", new InputTemplateDirectiveModelBean());
    	configuration.setSharedVariable("a", new HyperlinkTemplateDirectiveModelBean());
    	configuration.setSharedVariable("button", new ButtonTemplateDirectiveModelBean());
    	configuration.setSharedVariable("div", new DivTemplateDirectiveModelBean());
    	if(multiEnterpriseConfiguration!=null) {
    		RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();
            config.setPatternParser(requestMappingHandlerMapping.getPatternParser());
            requestMappingHandlerMapping.registerMapping(RequestMappingInfo
    				.paths(AuthConstants.URI_TO_ENTERPRISE_SELECTION)
                    .methods(RequestMethod.GET)
                    .options(config)
                    .build(), multiEnterpriseConfiguration, MultiEnterpriseConfiguration.class.getMethod("toEnterpriseSelection"));
    		requestMappingHandlerMapping.registerMapping(RequestMappingInfo
    				.paths("/auth/enterprise/select")
                    .options(config)
                    .build(), multiEnterpriseConfiguration, MultiEnterpriseConfiguration.class.getMethod("selectEnterprise", HttpServletRequest.class, Long.class));
    	}
	}
}