package com.quincy.auth;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.auth.freemarker.ButtonTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.DivTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.HyperlinkTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.InputTemplateDirectiveModelBean;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

@Configuration
public class AuthCommonInitialization {
	@Autowired
    private freemarker.template.Configuration configuration;
	@Resource(name = InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		configuration.setSharedVariable("input", new InputTemplateDirectiveModelBean());
    	configuration.setSharedVariable("a", new HyperlinkTemplateDirectiveModelBean());
    	configuration.setSharedVariable("button", new ButtonTemplateDirectiveModelBean());
    	configuration.setSharedVariable("div", new DivTemplateDirectiveModelBean());
	}

	private String authCenter() {
		String authCenter = CommonHelper.trim(properties.getProperty("auth.center"));
		return authCenter==null?"":authCenter;
	}

	@Bean("signinUrl")
	public String signinUrl() {
		return authCenter()+"/auth/signin/broker";
	}

	@Bean("denyUrl")
	public String denyUrl() {
		return authCenter()+"/auth/deny";
	}

	@PreDestroy
	private void destroy() {
		
	}
}