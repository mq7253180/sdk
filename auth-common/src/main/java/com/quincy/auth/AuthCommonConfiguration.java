package com.quincy.auth;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.auth.freemarker.ButtonTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.DivTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.HyperlinkTemplateDirectiveModelBean;
import com.quincy.auth.freemarker.InputTemplateDirectiveModelBean;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@Configuration
public class AuthCommonConfiguration {
	@Autowired
    private freemarker.template.Configuration configuration;
	@Value("${auth.center:}")
	private String authCenter;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		configuration.setSharedVariable("input", new InputTemplateDirectiveModelBean());
    	configuration.setSharedVariable("a", new HyperlinkTemplateDirectiveModelBean());
    	configuration.setSharedVariable("button", new ButtonTemplateDirectiveModelBean());
    	configuration.setSharedVariable("div", new DivTemplateDirectiveModelBean());
	}

	@Bean("signinUrl")
	public String signinUrl() {
		return authCenter+"/auth/signin/broker";
	}

	@Bean("denyUrl")
	public String denyUrl() {
		return authCenter+"/auth/deny";
	}

	@PreDestroy
	private void destroy() {
		
	}

	@Bean
    public HttpSessionListener httpSessionListener() {
		return new HttpSessionListener() {
			@Override
			public void sessionCreated(HttpSessionEvent hse) {
				AuthSessionHolder.SESSIONS.put(hse.getSession().getId(), hse.getSession());
			}

			@Override
			public void sessionDestroyed(HttpSessionEvent hse) {
				AuthSessionHolder.SESSIONS.remove(hse.getSession().getId());
			}
		};
	}
}