package com.quincy.auth;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.quincy.auth.freemarker.ButtonTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.DivTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.HyperlinkTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.InputTemplateDirectiveModelBean;

@Configuration
public class AuthCommonConfiguration {
	@Autowired
    private freemarker.template.Configuration configuration;
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		configuration.setSharedVariable("input", new InputTemplateDirectiveModelBean());
    	configuration.setSharedVariable("a", new HyperlinkTemplateDirectiveModelBean());
    	configuration.setSharedVariable("button", new ButtonTemplateDirectiveModelBean());
    	configuration.setSharedVariable("div", new DivTemplateDirectiveModelBean());
    	MultiEnterpriseConfiguration c = applicationContext.getBean(MultiEnterpriseConfiguration.class);
    	if(c!=null) {
    		RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();
            config.setPatternParser(requestMappingHandlerMapping.getPatternParser());
    		RequestMappingInfo requestMappingInfo = RequestMappingInfo
    				.paths(AuthConstants.URI_TO_ENTERPRISE_SELECTION)
                    .methods(RequestMethod.GET)
                    .options(config)
                    .build();
    		requestMappingHandlerMapping.registerMapping(requestMappingInfo, c, MultiEnterpriseConfiguration.class.getMethod("toEnterpriseSelection"));
    	}
	}
}