package com.quincy.core;

import java.util.Properties;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.sftp.ChannelSftpSource;
import com.quincy.core.sftp.PoolableChannelSftp;
import com.quincy.core.sftp.PoolableChannelSftpFactory;
import com.quincy.core.sftp.impl.ChannelSftpSourceImpl;
import com.quincy.sdk.Constants;

@Configuration
public class SFTPApplicationContext {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@Bean
	public ChannelSftpSource createChannelSftpSource() {
		String hostPrimary = properties.getProperty("sftp.host.primary");
	    String hostSecondary = properties.getProperty("sftp.host.secondary");
	    int port = Integer.parseInt(properties.getProperty("sftp.port"));
	    String username = properties.getProperty("sftp.username");
		GenericObjectPoolConfig<PoolableChannelSftp> pc = new GenericObjectPoolConfig<PoolableChannelSftp>();
		pc.setMaxTotal(Integer.parseInt(properties.getProperty("sftp.pool.maxTotal")));
		pc.setMaxIdle(Integer.parseInt(properties.getProperty("sftp.pool.maxIdle")));
		pc.setMinIdle(Integer.parseInt(properties.getProperty("sftp.pool.minIdle")));
		pc.setMaxWaitMillis(Long.parseLong(properties.getProperty("sftp.pool.maxWaitMillis")));
		pc.setMinEvictableIdleTimeMillis(Long.parseLong(properties.getProperty("sftp.pool.minEvictableIdleTimeMillis")));
		pc.setTimeBetweenEvictionRunsMillis(Long.parseLong(properties.getProperty("sftp.pool.timeBetweenEvictionRunsMillis")));
		pc.setNumTestsPerEvictionRun(Integer.parseInt(properties.getProperty("sftp.pool.numTestsPerEvictionRun")));
		pc.setBlockWhenExhausted(Boolean.parseBoolean(properties.getProperty("sftp.pool.blockWhenExhausted")));
		pc.setTestOnBorrow(Boolean.parseBoolean(properties.getProperty("sftp.pool.testOnBorrow")));
		pc.setTestOnCreate(Boolean.parseBoolean(properties.getProperty("sftp.pool.testOnCreate")));
		pc.setTestOnReturn(Boolean.parseBoolean(properties.getProperty("sftp.pool.testOnReturn")));
		pc.setTestWhileIdle(Boolean.parseBoolean(properties.getProperty("sftp.pool.testWhileIdle")));
//		pc.setEvictionPolicyClassName(MyEvictionPolicy.class.getName());
		PoolableChannelSftpFactory f = new PoolableChannelSftpFactory(hostPrimary, hostSecondary, port, username);
		AbandonedConfig ac = new AbandonedConfig();
		ac.setRemoveAbandonedOnMaintenance(Boolean.parseBoolean(properties.getProperty("sftp.pool.removeAbandonedOnMaintenance")));//在Maintenance的时候检查是否有泄漏
		ac.setRemoveAbandonedOnBorrow(Boolean.parseBoolean(properties.getProperty("sftp.pool.removeAbandonedOnBorrow")));//borrow的时候检查泄漏
		ac.setRemoveAbandonedTimeout(Integer.parseInt(properties.getProperty("sftp.pool.removeAbandonedTimeout")));//如果一个对象borrow之后n秒还没有返还给pool，认为是泄漏的对象
		ac.setLogAbandoned(Boolean.parseBoolean(properties.getProperty("sftp.pool.logAbandoned")));
		ac.setUseUsageTracking(Boolean.parseBoolean(properties.getProperty("sftp.pool.useUsageTracking")));
		ac.setRequireFullStackTrace(Boolean.parseBoolean(properties.getProperty("sftp.pool.requireFullStackTrace")));
		ChannelSftpSource s = new ChannelSftpSourceImpl(f, pc, ac);
		return s;
	}

	@Autowired
	private ChannelSftpSource channelSftpSource;

	@PreDestroy
	public void destroy() {
		if(channelSftpSource!=null)
			channelSftpSource.close();
	}
}