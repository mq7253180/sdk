package com.quincy.auth;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.quincy.auth.freemarker.ButtonTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.DivTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.HyperlinkTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.InputTemplateDirectiveModelBean;

@Configuration
public class AuthCommonConfiguration {
	@Autowired
    private freemarker.template.Configuration configuration;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		configuration.setSharedVariable("input", new InputTemplateDirectiveModelBean());
    	configuration.setSharedVariable("a", new HyperlinkTemplateDirectiveModelBean());
    	configuration.setSharedVariable("button", new ButtonTemplateDirectiveModelBean());
    	configuration.setSharedVariable("div", new DivTemplateDirectiveModelBean());
	}
}