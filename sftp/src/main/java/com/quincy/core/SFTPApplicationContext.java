package com.quincy.core;

import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.sftp.ChannelSftpSource;
import com.quincy.core.sftp.MyEvictionPolicy;
import com.quincy.core.sftp.PoolableChannelSftp;
import com.quincy.core.sftp.PooledChannelSftpFactory;
import com.quincy.core.sftp.impl.ChannelSftpSourceImpl;
import com.quincy.sdk.Constants;

@Configuration
public class SFTPApplicationContext {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@Bean
	public ChannelSftpSource createChannelSftpSource() {
		String hostPrimary = properties.getProperty("sftp_connection.host.primary");
	    String hostSecondary = properties.getProperty("sftp_connection.host.secondary");
	    int port = Integer.parseInt(properties.getProperty("sftp_connection.port"));
	    String username = properties.getProperty("sftp_connection.username");
	    Integer maxTotal = Integer.valueOf(properties.getProperty("sftp_connection.pool.max_total"));
	    Integer maxIdle = Integer.valueOf(properties.getProperty("sftp_connection.pool.max_idle"));
	    Integer minIdle = Integer.valueOf(properties.getProperty("sftp_connection.pool.min_idle"));
	    Long maxWaitMillis = Long.valueOf(properties.getProperty("sftp_connection.pool.max_wait_millis"));
	    Long softMinEvictableIdleTimeMillis = Long.valueOf(properties.getProperty("sftp_connection.pool.soft_min_evictable_idle_time_millis"));
	    Long timeBetweenEvictionRunsMillis = Long.valueOf(properties.getProperty("sftp_connection.pool.time_between_eviction_runs_millis"));
		GenericObjectPoolConfig<PoolableChannelSftp> pc = new GenericObjectPoolConfig<PoolableChannelSftp>();
    	pc.setMaxIdle(maxIdle);
    	pc.setMinIdle(minIdle);
    	pc.setMaxTotal(maxTotal);
//    	pc.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
//    	pc.setTestOnCreate(true);
    	pc.setTestOnBorrow(true);
//    	pc.setTestOnReturn(true);
//    	pc.setTestWhileIdle(true);
//    	pc.setBlockWhenExhausted(true);
    	pc.setMaxWaitMillis(maxWaitMillis);//最大等待时间
    	pc.setMinEvictableIdleTimeMillis(-1);//最小空闲时间
    	pc.setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);//最小空闲时间
    	pc.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);//驱逐器触发间隔
//    	pc.setEvictorShutdownTimeoutMillis(evictorShutdownTimeoutMillis);
    	pc.setEvictionPolicyClassName(MyEvictionPolicy.class.getName());
    	PooledChannelSftpFactory f = new PooledChannelSftpFactory(hostPrimary, hostSecondary, port, username);
    	AbandonedConfig ac = new AbandonedConfig();
//    	ac.setRemoveAbandonedOnMaintenance(true); //在Maintenance的时候检查是否有泄漏
//    	ac.setRemoveAbandonedOnBorrow(true); //borrow 的时候检查泄漏
//    	ac.setRemoveAbandonedTimeout(10);
		ChannelSftpSource s = new ChannelSftpSourceImpl(f, pc, ac);
		return s;
	}
}
