package com.quincy.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.servlet.LocaleResolver;

import com.quincy.core.web.GlobalHandlerExceptionResolver;
import com.quincy.core.web.GlobalLocaleResolver;
import com.quincy.sdk.helper.CommonHelper;

@Configuration
public class CommonPrimaryConfiguration {//implements TransactionManagementConfigurer {
	@Bean
    public MessageSource messageSource() throws IOException {
		Map<String, String> map = new HashMap<String, String>();
		new ClassPathHandler() {
			@Override
			protected void run(List<Resource> resources) {
				for(int i=0;i<resources.size();i++) {
					Resource resource = resources.get(i);
					int indexOf = resource.getFilename().indexOf("_");
					indexOf = indexOf<0?resource.getFilename().indexOf("."):indexOf;
					String name = resource.getFilename().substring(0, indexOf);
					map.put(name, "classpath:i18n/"+name);
				}
			}
		}.start("classpath*:i18n/*");
		String[] basenames = new String[map.size()];
		basenames = map.values().toArray(basenames);
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setCacheSeconds(1800);
        messageSource.setBasenames(basenames);
        return messageSource;
    }

//	@Bean(InnerConstants.BEAN_NAME_PROPERTIES)
	public PropertiesFactoryBean properties() throws IOException {
		List<Resource> resourceList = new ArrayList<Resource>();
		new ClassPathHandler() {
			@Override
			protected void run(List<Resource> resources) {
				resourceList.addAll(resources);
			}
		}.start("classpath*:application.properties", "classpath*:application-*.properties");
		Resource[] locations = new Resource[resourceList.size()];
		locations = resourceList.toArray(locations);
		PropertiesFactoryBean bean = new PropertiesFactoryBean();
		bean.setLocations(locations);
		bean.afterPropertiesSet();
		return bean;
	}

	public static abstract class ClassPathHandler {
		protected abstract void run(List<Resource> resources);

		public void start(String... locationPatterns) throws IOException {
			PathMatchingResourcePatternResolver r = new PathMatchingResourcePatternResolver();
			List<Resource> resourceList = new ArrayList<Resource>(50);
			for(String locationPattern:locationPatterns) {
				Resource[] resources = r.getResources(locationPattern);
				for(Resource resource:resources) {
					resourceList.add(resource);
				}
			}
			this.run(resourceList);
		}
	}

	@Value("#{'${locales}'.split(',')}")
	private String[] supportedLocales;

	@PostConstruct
	public void init() {
		CommonHelper.SUPPORTED_LOCALES = supportedLocales;
	}

	@Bean("globalLocaleResolver")
    public LocaleResolver localeResolver() {
        return new GlobalLocaleResolver();
    }

	@Bean
	public GlobalHandlerExceptionResolver globalHandlerExceptionResolver() {
		return new GlobalHandlerExceptionResolver();
	}

	@Value("${mail.smtp.auth}")
	private String smtpAuth;
	@Value("${mail.smtp.starttls.enable}")
	private String smtpStarttlsEnable;
	@Value("${mail.smtp.host}")
	private String smtpHost;

	@Bean(InnerConstants.BEAN_NAME_PROPERTIES)
	public Properties mailProperties() {
		Properties properties = new Properties();
		properties.setProperty("mail.transport.protocol", "smtp");
		properties.setProperty("mail.host", smtpHost);
		properties.setProperty("mail.smtp.auth", smtpAuth);
		properties.setProperty("mail.smtp.starttls.enable", smtpStarttlsEnable);
		return properties;
	}

	@Value("${threadPool.corePoolSize}")
	private int corePoolSize;
	@Value("${threadPool.maximumPoolSize}")
	private int maximumPoolSize;
	@Value("${threadPool.keepAliveTimeSeconds}")
	private int keepAliveTimeSeconds;
	@Value("${threadPool.blockingQueueCapacity}")
	private int blockingQueueCapacity;

	@Bean(InnerConstants.BEAN_NAME_SYS_THREAD_POOL)
	public ThreadPoolExecutor threadPoolExecutor() {
		BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<Runnable>(blockingQueueCapacity);
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTimeSeconds, TimeUnit.SECONDS, blockingQueue);
		return threadPoolExecutor;
	}
}