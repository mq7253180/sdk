package com.quincy.core;

import javax.annotation.Resource;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.zookeeper.Watcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.zookeeper.ContextConstants;
import com.quincy.core.zookeeper.ZooKeeperSourceBean;
import com.quincy.sdk.ZKContext;
import com.quincy.sdk.helper.CommonHelper;

@Configuration
public class ZooKeeperConfiguration implements ZKContext {
	@Value("${spring.application.name}")
	private String applicationName;
	@Value("${zk.url}")
	private String url;
	@Value("${zk.timeout}")
	private int timeout;
	@Value("${zk.watcher}")
	private String watcher;
	@Autowired
	private GenericObjectPoolConfig poolCfg;
	@Autowired
	private AbandonedConfig abandonedCfg;
	@Autowired
	private ApplicationContext applicationContext;

	@Bean
	public ZooKeeperSourceBean zkSourceBean() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Watcher w = applicationContext.getBean(CommonHelper.trim(watcher), Watcher.class);
		ZooKeeperSourceBean b = new ZooKeeperSourceBean(url, timeout, w, poolCfg, abandonedCfg);
		b.afterPropertiesSet();
		return b;
	}

	@Bean("zookeeperRootNode")
	public String zookeeperRootNode() {
		return "/"+applicationName;
	}

	@Autowired
	@Qualifier("zookeeperRootNode")
	private String zookeeperRootNode;

	@Bean("zookeeperSynchronizationNode")
	public String zookeeperSynchronizationNode() {
		return zookeeperRootNode+"/"+ContextConstants.SYN_NODE;
	}

	@Autowired
	@Qualifier("zookeeperSynchronizationNode")
	private String zookeeperSynchronizationNode;

	@Override
	public String getRootPath() {
		return zookeeperRootNode;
	}

	@Override
	public String getSynPath() {
		return zookeeperSynchronizationNode;
	}
}