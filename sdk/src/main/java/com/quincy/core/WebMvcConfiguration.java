package com.quincy.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import com.quincy.sdk.Constants;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.view.GlobalHandlerMethodReturnValueHandler;
import com.quincy.sdk.view.GlobalLocaleResolver;
import com.quincy.sdk.view.I18NInterceptor;
import com.quincy.sdk.view.StaticInterceptor;
import com.quincy.sdk.view.freemarker.AttributeTemplateDirectiveModelBean;
import com.quincy.sdk.view.freemarker.I18NTemplateDirectiveModelBean;
import com.quincy.sdk.view.freemarker.LocaleTemplateDirectiveModelBean;
import com.quincy.sdk.view.freemarker.PropertiesTemplateDirectiveModelBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class WebMvcConfiguration extends WebMvcConfigurationSupport implements InitializingBean {
	@Autowired
    private RequestMappingHandlerAdapter adapter;
	@Autowired
	private ApplicationContext applicationContext;
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
    private Properties properties;
	@Value("${env}")
	private String env;

	@Override
	protected void addInterceptors(InterceptorRegistry registry) {
		if(Constants.ENV_DEV.equals(env))
			registry.addInterceptor(new StaticInterceptor()).addPathPatterns("/static/**");
		registry.addInterceptor(new I18NInterceptor()).addPathPatterns("/**");
		String interceptorImpl = CommonHelper.trim(properties.getProperty("impl.auth.interceptor"));
		if(interceptorImpl!=null) {
			HandlerInterceptorAdapter handlerInterceptorAdapter = applicationContext.getBean("interceptorImpl", HandlerInterceptorAdapter.class);
			registry.addInterceptor(handlerInterceptorAdapter).addPathPatterns("/**");
		}
	}

    @Override
    public void afterPropertiesSet() throws Exception {
        List<HandlerMethodReturnValueHandler> returnValueHandlers = adapter.getReturnValueHandlers();
        List<HandlerMethodReturnValueHandler> handlers = new ArrayList<HandlerMethodReturnValueHandler>(returnValueHandlers);
        decorateHandlers(handlers);
        adapter.setReturnValueHandlers(handlers);
    }

    private void decorateHandlers(List<HandlerMethodReturnValueHandler> handlers) {
        for(HandlerMethodReturnValueHandler handler:handlers) {
            if(handler instanceof RequestResponseBodyMethodProcessor) {
            	String cluster = CommonHelper.trim(properties.getProperty("impl.auth.interceptor"));
            	HandlerMethodReturnValueHandler decorator = new GlobalHandlerMethodReturnValueHandler(handler, applicationContext, cluster);
                int index = handlers.indexOf(handler);
                handlers.set(index, decorator);
                break;
            }
        }
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new GlobalLocaleResolver();
    }

    @PostConstruct
    public void freeMarkerConfigurer() {
    	freemarker.template.Configuration configuration = null;
    	try {
    		configuration = applicationContext.getBean(freemarker.template.Configuration.class);
    	} catch(NoSuchBeanDefinitionException e) {
    		log.warn("===================NO_FREEMARKER");
    	}
    	if(configuration!=null) {
    		configuration.setSharedVariable("attr", new AttributeTemplateDirectiveModelBean());
        	configuration.setSharedVariable("i18n", new I18NTemplateDirectiveModelBean(properties));
        	configuration.setSharedVariable("property", new PropertiesTemplateDirectiveModelBean(properties));
        	configuration.setSharedVariable("locale", new LocaleTemplateDirectiveModelBean());
    	}
    }

    /*@Bean
    public ViewResolver viewResolver() {
        FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
        resolver.setCache(true);
        resolver.setSuffix(".ftl");
        resolver.setContentType("text/html; charset=UTF-8");
        resolver.setAllowSessionOverride(true);
        return resolver;
    }*/

    /*@Bean
    public FreeMarkerConfigurer freeMarkerConfigurer() {
		String path = null;
		if("dev".equals(env)) {
			path = this.getClass().getResource("/").getPath();
			path = path.substring(0, path.indexOf("springboot/target/classes"))+"springboot/src/main/view/page/";
		} else {
			path = ftlLocation;
		}
		path = "file:"+path;
		log.info("VIEW_LOCATION====================="+path);
		Map<String, Object> variables = new HashMap<String, Object>(3);
		variables.put("attr", new AttributeTemplateDirectiveModelBean());
		variables.put("i18n", new I18NTemplateDirectiveModelBean(properties));
		variables.put("property", new PropertiesTemplateDirectiveModelBean(properties));
		configuration.setSharedVariable("attr", new AttributeTemplateDirectiveModelBean());
		configuration.setSharedVariable("i18n", new I18NTemplateDirectiveModelBean(properties));
		configuration.setSharedVariable("property", new PropertiesTemplateDirectiveModelBean(properties));
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        configurer.setTemplateLoaderPaths(path);
        configurer.setDefaultEncoding("UTF-8");
        configurer.setFreemarkerVariables(variables);
        return configurer;
    }*/
}