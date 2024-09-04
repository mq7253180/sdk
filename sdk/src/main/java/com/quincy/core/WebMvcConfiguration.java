package com.quincy.core;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.quincy.core.web.GeneralInterceptor;
import com.quincy.core.web.QuincyAuthInterceptor;
import com.quincy.core.web.SignatureInterceptor;
import com.quincy.core.web.StaticInterceptor;
import com.quincy.core.web.VCodeController;
import com.quincy.core.web.freemarker.AttributeTemplateDirectiveModelBean;
import com.quincy.core.web.freemarker.I18NTemplateDirectiveModelBean;
import com.quincy.core.web.freemarker.LocaleTemplateDirectiveModelBean;
import com.quincy.core.web.freemarker.PropertiesTemplateDirectiveModelBean;
import com.quincy.sdk.Constants;
import com.quincy.sdk.annotation.CustomizedInterceptor;

@Configuration
public class WebMvcConfiguration extends WebMvcConfigurationSupport {
	@Autowired
	private ApplicationContext applicationContext;
	@Value("${env}")
	private String env;
	@Autowired(required = false)
	private QuincyAuthInterceptor quincyAuthInterceptor;
	@Autowired(required = false)
	private PublicKeyGetter publicKeyGetter;
	@Autowired
	private VCodeController vCodeInterceptor;
	private final static String[] EXCLUDE_PATH_PATTERNS = new String[] {"/static/**", "/vcode/**", "/auth/**", "/failure", "/success"};

	@Override
	protected void addInterceptors(InterceptorRegistry registry) {
		if(Constants.ENV_DEV.equals(env))
			registry.addInterceptor(new StaticInterceptor()).addPathPatterns("/static/**");
		registry.addInterceptor(new GeneralInterceptor()).addPathPatterns("/**");
		if(publicKeyGetter!=null)
			registry.addInterceptor(new SignatureInterceptor(publicKeyGetter)).addPathPatterns("/**").excludePathPatterns(EXCLUDE_PATH_PATTERNS);
		registry.addInterceptor(vCodeInterceptor).addPathPatterns("/**").excludePathPatterns(EXCLUDE_PATH_PATTERNS);
		if(quincyAuthInterceptor!=null) {
			HandlerInterceptorAdapter handlerInterceptorAdapter = (HandlerInterceptorAdapter)quincyAuthInterceptor;
			registry.addInterceptor(handlerInterceptorAdapter).addPathPatterns("/**").excludePathPatterns(EXCLUDE_PATH_PATTERNS);
		}
		Map<String, Object> map = applicationContext.getBeansWithAnnotation(CustomizedInterceptor.class);
		for(Object interceptor:map.values()) {
			CustomizedInterceptor annotation = interceptor.getClass().getDeclaredAnnotation(CustomizedInterceptor.class);
			InterceptorRegistration registration = registry.addInterceptor((HandlerInterceptor)interceptor)
					.addPathPatterns(annotation.pathPatterns())
					.excludePathPatterns(EXCLUDE_PATH_PATTERNS)
					.order(annotation.order());
			String[] excludePathPatterns = annotation.excludePathPatterns();
			if(excludePathPatterns!=null&&excludePathPatterns.length>0)
				registration.excludePathPatterns(excludePathPatterns);
		}
	}

    @Autowired
    private freemarker.template.Configuration freemarkerCfg;

    @PostConstruct
    public void freeMarkerConfigurer() {
    	freemarkerCfg.setSharedVariable("attr", new AttributeTemplateDirectiveModelBean());
		freemarkerCfg.setSharedVariable("i18n", new I18NTemplateDirectiveModelBean(applicationContext.getEnvironment()));
		freemarkerCfg.setSharedVariable("property", new PropertiesTemplateDirectiveModelBean(applicationContext.getEnvironment()));
		freemarkerCfg.setSharedVariable("locale", new LocaleTemplateDirectiveModelBean());
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