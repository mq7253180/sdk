package com.quincy.core;

import javax.annotation.PreDestroy;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.sftp.ChannelSftpSource;
import com.quincy.core.sftp.PoolableChannelSftpFactory;
import com.quincy.core.sftp.impl.ChannelSftpSourceImpl;

@Configuration
public class SFTPApplicationContext {
	@Autowired
	private GenericObjectPoolConfig poolCfg;
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

	/**
	 * 防止脱离Spring Boot启动时抛异常: Requested bean is currently in creation: Is there an unresolvable circular reference?
	 */
	@Bean
	public Object xxxObjSftp() {
		return new Object();
	}

	@Bean
	public ChannelSftpSource createChannelSftpSource() {
		PoolableChannelSftpFactory f = new PoolableChannelSftpFactory(host, port, username, privateKey);
		ChannelSftpSource s = new ChannelSftpSourceImpl(f, poolCfg, abandonedCfg);
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