package com.quincy.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.quincy.sdk.Constants;
import com.quincy.sdk.PoolParams;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class CommonApplicationContext {//implements TransactionManagementConfigurer {
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

	@Bean(Constants.BEAN_NAME_PROPERTIES)
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
		log.warn("====================PROPERTIES_FACTORY_BEAN_CREATED");
		return bean;
	}

	private abstract class ClassPathHandler {
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
		for(String l:supportedLocales) {
			log.warn("SUPPORTED_LOCALE--------------{}", l);
		}
	}

	@javax.annotation.Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@Bean
	public PoolParams poolParams() {
		PoolParams poolParams = new PoolParams();
		poolParams.setMaxTotal(Integer.parseInt(properties.getProperty("pool.maxTotal")));
		poolParams.setMaxIdle(Integer.parseInt(properties.getProperty("pool.maxIdle")));
		poolParams.setMinIdle(Integer.parseInt(properties.getProperty("pool.minIdle")));
		poolParams.setMaxWaitMillis(Long.parseLong(properties.getProperty("pool.maxWaitMillis")));
		poolParams.setMinEvictableIdleTimeMillis(Long.parseLong(properties.getProperty("pool.minEvictableIdleTimeMillis")));
		poolParams.setTimeBetweenEvictionRunsMillis(Long.parseLong(properties.getProperty("pool.timeBetweenEvictionRunsMillis")));
		poolParams.setNumTestsPerEvictionRun(Integer.parseInt(properties.getProperty("pool.numTestsPerEvictionRun")));
//		poolParams.setBlockWhenExhausted(Boolean.parseBoolean(properties.getProperty("pool.blockWhenExhausted")));
		poolParams.setTestOnBorrow(Boolean.parseBoolean(properties.getProperty("pool.testOnBorrow")));
		poolParams.setTestWhileIdle(Boolean.parseBoolean(properties.getProperty("pool.testWhileIdle")));
		poolParams.setTestOnReturn(Boolean.parseBoolean(properties.getProperty("pool.testOnReturn")));
//		poolParams.setRemoveAbandonedOnMaintenance(Boolean.parseBoolean(properties.getProperty("pool.removeAbandonedOnMaintenance")));
//		poolParams.setRemoveAbandonedOnBorrow(Boolean.parseBoolean(properties.getProperty("pool.removeAbandonedOnBorrow")));
//		poolParams.setRemoveAbandonedTimeout(Integer.parseInt(properties.getProperty("pool.removeAbandonedTimeout")));
		return poolParams;
	}
}