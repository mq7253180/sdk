package com.quincy.core;

import javax.annotation.PreDestroy;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.sftp.ChannelSftpSource;
import com.quincy.core.sftp.PoolableChannelSftp;
import com.quincy.core.sftp.PoolableChannelSftpFactory;
import com.quincy.core.sftp.impl.ChannelSftpSourceImpl;

@Configuration
public class SFTPApplicationContext {
	@Autowired
	private GenericObjectPoolConfig<?> poolCfg;
	@Autowired
	private AbandonedConfig abandonedCfg;
	@Value("${sftp.host}")
	private String host;
	@Value("${sftp.port}")
	private int port;
	@Value("${sftp.username}")
	private String username;
	@Value("${sftp.privateKey}")
	private String privateKey;

	@Bean
	public ChannelSftpSource createChannelSftpSource() {
		GenericObjectPoolConfig<PoolableChannelSftp> cfg = new GenericObjectPoolConfig<PoolableChannelSftp>();
		cfg.setMaxTotal(poolCfg.getMaxTotal());
		cfg.setMaxIdle(poolCfg.getMaxIdle());
		cfg.setMinIdle(poolCfg.getMinIdle());
		cfg.setMaxWaitMillis(poolCfg.getMaxWaitMillis());
		cfg.setMinEvictableIdleTimeMillis(poolCfg.getMinEvictableIdleTimeMillis());
		cfg.setTimeBetweenEvictionRunsMillis(poolCfg.getTimeBetweenEvictionRunsMillis());
		cfg.setNumTestsPerEvictionRun(poolCfg.getNumTestsPerEvictionRun());
		cfg.setBlockWhenExhausted(poolCfg.getBlockWhenExhausted());
		cfg.setTestOnBorrow(poolCfg.getTestOnBorrow());
		cfg.setTestOnCreate(poolCfg.getTestOnCreate());
		cfg.setTestOnReturn(poolCfg.getTestOnReturn());
		cfg.setTestWhileIdle(poolCfg.getTestWhileIdle());
		cfg.setFairness(poolCfg.getFairness());
		cfg.setLifo(poolCfg.getLifo());
		cfg.setEvictionPolicyClassName(poolCfg.getEvictionPolicyClassName());
		cfg.setEvictorShutdownTimeoutMillis(poolCfg.getEvictorShutdownTimeoutMillis());
		cfg.setSoftMinEvictableIdleTimeMillis(poolCfg.getSoftMinEvictableIdleTimeMillis());
		cfg.setJmxEnabled(poolCfg.getJmxEnabled());
		cfg.setJmxNameBase(poolCfg.getJmxNameBase());
		cfg.setJmxNamePrefix(poolCfg.getJmxNamePrefix());
		PoolableChannelSftpFactory f = new PoolableChannelSftpFactory(host, port, username, privateKey);
		ChannelSftpSource s = new ChannelSftpSourceImpl(f, cfg, abandonedCfg);
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