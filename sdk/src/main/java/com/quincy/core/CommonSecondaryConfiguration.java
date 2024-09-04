package com.quincy.core;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonSecondaryConfiguration {
	@Value("${pool.maxTotal:#{null}}")
	private Integer maxTotal;
	@Value("${pool.maxIdle:#{null}}")
	private Integer maxIdle;
	@Value("${pool.minIdle:#{null}}")
	private Integer minIdle;
	@Value("${pool.maxWaitMillis:#{null}}")
	private Long maxWaitMillis;
	@Value("${pool.minEvictableIdleTimeMillis:#{null}}")
	private Long minEvictableIdleTimeMillis;
	@Value("${pool.timeBetweenEvictionRunsMillis:#{null}}")
	private Long timeBetweenEvictionRunsMillis;
	@Value("${pool.numTestsPerEvictionRun:#{null}}")
	private Integer numTestsPerEvictionRun;
	@Value("${pool.blockWhenExhausted:#{null}}")
	private Boolean blockWhenExhausted;
	@Value("${pool.testOnBorrow:#{null}}")
	private Boolean testOnBorrow;
	@Value("${pool.testOnCreate:#{null}}")
	private Boolean testOnCreate;
	@Value("${pool.testOnReturn:#{null}}")
	private Boolean testOnReturn;
	@Value("${pool.testWhileIdle:#{null}}")
	private Boolean testWhileIdle;
	@Value("${pool.fairness:#{null}}")
	private Boolean fairness;
	@Value("${pool.lifo:#{null}}")
	private Boolean lifo;
	@Value("${pool.evictionPolicyClassName:#{null}}")
	private String evictionPolicyClassName;
	@Value("${pool.softMinEvictableIdleTimeMillis:#{null}}")
	private Long softMinEvictableIdleTimeMillis;
	@Value("${pool.jmxEnabled:#{null}}")
	private Boolean jmxEnabled;
	@Value("${pool.jmxNameBase:#{null}}")
	private String jmxNameBase;
	@Value("${pool.jmxNamePrefix:#{null}}")
	private String jmxNamePrefix;

	@Bean
	public GenericObjectPoolConfig genericObjectPoolConfig() {
//		String maxTotal = CommonHelper.trim(properties.getProperty("pool.maxTotal"));
//		String maxIdle = CommonHelper.trim(properties.getProperty("pool.maxIdle"));
//		String minIdle = CommonHelper.trim(properties.getProperty("pool.minIdle"));
//		String maxWaitMillis = CommonHelper.trim(properties.getProperty("pool.maxWaitMillis"));
//		String minEvictableIdleTimeMillis = CommonHelper.trim(properties.getProperty("pool.minEvictableIdleTimeMillis"));
//		String timeBetweenEvictionRunsMillis = CommonHelper.trim(properties.getProperty("pool.timeBetweenEvictionRunsMillis"));
//		String numTestsPerEvictionRun = CommonHelper.trim(properties.getProperty("pool.numTestsPerEvictionRun"));
//		String blockWhenExhausted = CommonHelper.trim(properties.getProperty("pool.blockWhenExhausted"));
//		String testOnBorrow = CommonHelper.trim(properties.getProperty("pool.testOnBorrow"));
//		String testOnCreate = CommonHelper.trim(properties.getProperty("pool.testOnCreate"));
//		String testOnReturn = CommonHelper.trim(properties.getProperty("pool.testOnReturn"));
//		String testWhileIdle = CommonHelper.trim(properties.getProperty("pool.testWhileIdle"));
//		String fairness = CommonHelper.trim(properties.getProperty("pool.fairness"));
//		String lifo = CommonHelper.trim(properties.getProperty("pool.lifo"));
//		String evictionPolicyClassName = CommonHelper.trim(properties.getProperty("pool.evictionPolicyClassName"));
//		String softMinEvictableIdleTimeMillis = CommonHelper.trim(properties.getProperty("pool.softMinEvictableIdleTimeMillis"));
//		String jmxEnabled = CommonHelper.trim(properties.getProperty("pool.jmxEnabled"));
//		String jmxNameBase = CommonHelper.trim(properties.getProperty("pool.jmxNameBase"));
//		String jmxNamePrefix = CommonHelper.trim(properties.getProperty("pool.jmxNamePrefix"));
		GenericObjectPoolConfig poolParams = new GenericObjectPoolConfig();
		if(maxTotal!=null)
			poolParams.setMaxTotal(maxTotal);
		if(maxIdle!=null)
			poolParams.setMaxIdle(maxIdle);
		if(minIdle!=null)
			poolParams.setMinIdle(minIdle);
		if(maxWaitMillis!=null)
			poolParams.setMaxWaitMillis(maxWaitMillis);
		if(minEvictableIdleTimeMillis!=null)
			poolParams.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		if(timeBetweenEvictionRunsMillis!=null)
			poolParams.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
		if(numTestsPerEvictionRun!=null)
			poolParams.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
		if(blockWhenExhausted!=null)
			poolParams.setBlockWhenExhausted(blockWhenExhausted);
		if(testOnBorrow!=null)
			poolParams.setTestOnBorrow(testOnBorrow);
		if(testOnCreate!=null)
			poolParams.setTestOnCreate(testOnCreate);
		if(testOnReturn!=null)
			poolParams.setTestOnReturn(testOnReturn);
		if(testWhileIdle!=null)
			poolParams.setTestWhileIdle(testWhileIdle);
		if(fairness!=null)
			poolParams.setFairness(fairness);
		if(lifo!=null)
			poolParams.setLifo(lifo);
		if(evictionPolicyClassName!=null)
			poolParams.setEvictionPolicyClassName(evictionPolicyClassName);
		if(softMinEvictableIdleTimeMillis!=null)
			poolParams.setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);
		if(jmxEnabled!=null)
			poolParams.setJmxEnabled(jmxEnabled);
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
	@Value("${pool.requireFullStackTrace:#{null}}")
	private String requireFullStackTrace;

	@Bean
	public AbandonedConfig abandonedConfig() {
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