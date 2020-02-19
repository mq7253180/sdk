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

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.quincy.sdk.Constants;
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

	@Value("${threadPool.corePoolSize}")
	private int corePoolSize;
	@Value("${threadPool.maximumPoolSize}")
	private int maximumPoolSize;
	@Value("${threadPool.keepAliveTimeSeconds}")
	private int keepAliveTimeSeconds;
	@Value("${threadPool.blockingQueueCapacity}")
	private int capacity;

	@Bean
	public ThreadPoolExecutor threadPoolExecutor() {
		BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<Runnable>(100000);
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTimeSeconds, TimeUnit.SECONDS, blockingQueue);;
		return threadPoolExecutor;
	}

	@javax.annotation.Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@Bean
	public GenericObjectPoolConfig<?> genericObjectPoolConfig() {
		String maxTotal = CommonHelper.trim(properties.getProperty("pool.maxTotal"));
		String maxIdle = CommonHelper.trim(properties.getProperty("pool.maxIdle"));
		String minIdle = CommonHelper.trim(properties.getProperty("pool.minIdle"));
		String maxWaitMillis = CommonHelper.trim(properties.getProperty("pool.maxWaitMillis"));
		String minEvictableIdleTimeMillis = CommonHelper.trim(properties.getProperty("pool.minEvictableIdleTimeMillis"));
		String timeBetweenEvictionRunsMillis = CommonHelper.trim(properties.getProperty("pool.timeBetweenEvictionRunsMillis"));
		String numTestsPerEvictionRun = CommonHelper.trim(properties.getProperty("pool.numTestsPerEvictionRun"));
		String blockWhenExhausted = CommonHelper.trim(properties.getProperty("pool.blockWhenExhausted"));
		String testOnBorrow = CommonHelper.trim(properties.getProperty("pool.testOnBorrow"));
		String testOnCreate = CommonHelper.trim(properties.getProperty("pool.testOnCreate"));
		String testOnReturn = CommonHelper.trim(properties.getProperty("pool.testOnReturn"));
		String testWhileIdle = CommonHelper.trim(properties.getProperty("pool.testWhileIdle"));
		String fairness = CommonHelper.trim(properties.getProperty("pool.fairness"));
		String lifo = CommonHelper.trim(properties.getProperty("pool.lifo"));
		String evictionPolicyClassName = CommonHelper.trim(properties.getProperty("pool.evictionPolicyClassName"));
		String evictorShutdownTimeoutMillis = CommonHelper.trim(properties.getProperty("pool.evictorShutdownTimeoutMillis"));
		String softMinEvictableIdleTimeMillis = CommonHelper.trim(properties.getProperty("pool.softMinEvictableIdleTimeMillis"));
		String jmxEnabled = CommonHelper.trim(properties.getProperty("pool.jmxEnabled"));
		String jmxNameBase = CommonHelper.trim(properties.getProperty("pool.jmxNameBase"));
		String jmxNamePrefix = CommonHelper.trim(properties.getProperty("pool.jmxNamePrefix"));
		GenericObjectPoolConfig<?> poolParams = new GenericObjectPoolConfig<Object>();
		if(maxTotal!=null)
			poolParams.setMaxTotal(Integer.parseInt(maxTotal));
		if(maxIdle!=null)
			poolParams.setMaxIdle(Integer.parseInt(maxIdle));
		if(minIdle!=null)
			poolParams.setMinIdle(Integer.parseInt(minIdle));
		if(maxWaitMillis!=null)
			poolParams.setMaxWaitMillis(Long.parseLong(maxWaitMillis));
		if(minEvictableIdleTimeMillis!=null)
			poolParams.setMinEvictableIdleTimeMillis(Long.parseLong(minEvictableIdleTimeMillis));
		if(timeBetweenEvictionRunsMillis!=null)
			poolParams.setTimeBetweenEvictionRunsMillis(Long.parseLong(timeBetweenEvictionRunsMillis));
		if(numTestsPerEvictionRun!=null)
			poolParams.setNumTestsPerEvictionRun(Integer.parseInt(numTestsPerEvictionRun));
		if(blockWhenExhausted!=null)
			poolParams.setBlockWhenExhausted(Boolean.parseBoolean(blockWhenExhausted));	
		if(testOnBorrow!=null)
			poolParams.setTestOnBorrow(Boolean.parseBoolean(testOnBorrow));
		if(testOnCreate!=null)
			poolParams.setTestOnCreate(Boolean.parseBoolean(testOnCreate));
		if(testOnReturn!=null)
			poolParams.setTestOnReturn(Boolean.parseBoolean(testOnReturn));
		if(testWhileIdle!=null)
			poolParams.setTestWhileIdle(Boolean.parseBoolean(testWhileIdle));
		if(fairness!=null)
			poolParams.setFairness(Boolean.parseBoolean(fairness));
		if(lifo!=null)
			poolParams.setLifo(Boolean.parseBoolean(lifo));
		if(evictionPolicyClassName!=null)
			poolParams.setEvictionPolicyClassName(evictionPolicyClassName);
		if(evictorShutdownTimeoutMillis!=null)
			poolParams.setEvictorShutdownTimeoutMillis(Long.parseLong(evictorShutdownTimeoutMillis));
		if(softMinEvictableIdleTimeMillis!=null)
			poolParams.setSoftMinEvictableIdleTimeMillis(Long.parseLong(softMinEvictableIdleTimeMillis));
		if(jmxEnabled!=null)
			poolParams.setJmxEnabled(Boolean.parseBoolean(jmxEnabled));
		if(jmxNameBase!=null)
			poolParams.setJmxNameBase(jmxNameBase);
		if(jmxNamePrefix!=null)
			poolParams.setJmxNamePrefix(jmxNamePrefix);
		return poolParams;
	}

	@Bean
	public AbandonedConfig abandonedConfig() {
		String removeAbandonedOnMaintenance = CommonHelper.trim(properties.getProperty("pool.removeAbandonedOnMaintenance"));
		String removeAbandonedOnBorrow = CommonHelper.trim(properties.getProperty("pool.removeAbandonedOnBorrow"));
		String removeAbandonedTimeout = CommonHelper.trim(properties.getProperty("pool.removeAbandonedTimeout"));
		String logAbandoned = CommonHelper.trim(properties.getProperty("pool.logAbandoned"));
		String useUsageTracking = CommonHelper.trim(properties.getProperty("pool.useUsageTracking"));
		String requireFullStackTrace = CommonHelper.trim(properties.getProperty("pool.requireFullStackTrace"));
		AbandonedConfig ac = new AbandonedConfig();
		if(removeAbandonedOnMaintenance!=null)
			ac.setRemoveAbandonedOnMaintenance(Boolean.parseBoolean(removeAbandonedOnMaintenance));//在Maintenance的时候检查是否有泄漏
		if(removeAbandonedOnBorrow!=null)
			ac.setRemoveAbandonedOnBorrow(Boolean.parseBoolean(removeAbandonedOnBorrow));//borrow的时候检查泄漏
		if(removeAbandonedTimeout!=null)
			ac.setRemoveAbandonedTimeout(Integer.parseInt(removeAbandonedTimeout));//如果一个对象borrow之后n秒还没有返还给pool，认为是泄漏的对象
		if(logAbandoned!=null)
			ac.setLogAbandoned(Boolean.parseBoolean(logAbandoned));
		if(useUsageTracking!=null)
			ac.setUseUsageTracking(Boolean.parseBoolean(useUsageTracking));
		if(requireFullStackTrace!=null)
			ac.setRequireFullStackTrace(Boolean.parseBoolean(requireFullStackTrace));
//		ac.setLogWriter(logWriter);
		return ac;
	}
}