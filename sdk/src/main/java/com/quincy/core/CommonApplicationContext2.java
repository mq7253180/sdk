package com.quincy.core;

import java.util.Properties;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.sdk.helper.CommonHelper;

@Configuration
public class CommonApplicationContext2 {
	@Autowired
	@Qualifier(InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@Bean
	public GenericObjectPoolConfig genericObjectPoolConfig() {
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
		String softMinEvictableIdleTimeMillis = CommonHelper.trim(properties.getProperty("pool.softMinEvictableIdleTimeMillis"));
		String jmxEnabled = CommonHelper.trim(properties.getProperty("pool.jmxEnabled"));
		String jmxNameBase = CommonHelper.trim(properties.getProperty("pool.jmxNameBase"));
		String jmxNamePrefix = CommonHelper.trim(properties.getProperty("pool.jmxNamePrefix"));
		GenericObjectPoolConfig poolParams = new GenericObjectPoolConfig();
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

	@Value("${pool.removeAbandonedOnMaintenance:#{null}}")
	private Boolean removeAbandonedOnMaintenance;
	@Value("${pool.removeAbandonedOnBorrow:#{null}}")
	private Boolean removeAbandonedOnBorrow;
	@Value("${pool.removeAbandonedTimeout:#{null}}")
	private Integer removeAbandonedTimeout;
	@Value("${pool.logAbandoned:#{null}}")
	private Boolean logAbandoned;
	@Value("${pool.useUsageTracking:#{null}}")
	private Boolean useUsageTracking;

	@Bean
	public AbandonedConfig abandonedConfig() {
		String requireFullStackTrace = CommonHelper.trim(properties.getProperty("pool.requireFullStackTrace"));
		AbandonedConfig ac = new AbandonedConfig();
		if(removeAbandonedOnMaintenance!=null)
			ac.setRemoveAbandonedOnMaintenance(removeAbandonedOnMaintenance);//在Maintenance的时候检查是否有泄漏
		if(removeAbandonedOnBorrow!=null)
			ac.setRemoveAbandonedOnBorrow(removeAbandonedOnBorrow);//borrow的时候检查泄漏
		if(removeAbandonedTimeout!=null)
			ac.setRemoveAbandonedTimeout(removeAbandonedTimeout);//如果一个对象borrow之后n秒还没有返还给pool，认为是泄漏的对象
		if(logAbandoned!=null)
			ac.setLogAbandoned(logAbandoned);
		if(useUsageTracking!=null)
			ac.setUseUsageTracking(useUsageTracking);
		/*if(requireFullStackTrace!=null)
			ac.setRequireFullStackTrace(Boolean.parseBoolean(requireFullStackTrace));*/
//		ac.setLogWriter(logWriter);
		return ac;
	}
}