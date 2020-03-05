package com.quincy.core;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.zookeeper.ContextConstants;
import com.quincy.core.zookeeper.ZooKeeperSource;
import com.quincy.core.zookeeper.ZooKeeperSourceBean;
import com.quincy.sdk.zookeeper.Context;

@Configuration
public class ZooKeeperApplicationContext implements Context {
	@Value("${spring.application.name}")
	private String applicationName;
	@Value("${zk.url}")
	private String url;
	@Value("${zk.timeout}")
	private int timeout;
	@Value("${zk.watcher}")
	private String watcher;
	@Value("#{'${zk.distributedLock.keys}'.split(',')}")
	private String[] distributedLockKeys;
	@Autowired
	private GenericObjectPoolConfig poolCfg;
	@Autowired
	private AbandonedConfig abandonedCfg;

	@Bean
	public ZooKeeperSourceBean zkSourceBean() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		ZooKeeperSourceBean b = new ZooKeeperSourceBean(url, timeout, watcher, poolCfg, abandonedCfg);
		b.afterPropertiesSet();
		return b;
	}

	@Autowired
	private ZooKeeperSource zooKeeperSource;

	@PostConstruct
	public void init() throws Exception {
		ZooKeeper zk = null;
		try {
			zk = zooKeeperSource.get();
			Stat stat = zk.exists(zookeeperRootNode, false);
			if(stat==null)
				zk.create(zookeeperRootNode, "Root".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			stat = zk.exists(zookeeperSynchronizationNode, false);
			if(stat==null)
				zk.create(zookeeperSynchronizationNode, "Distributed Locks".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			for(String key:distributedLockKeys) {
				String path = zookeeperSynchronizationNode+"/"+key;
				stat = zk.exists(path, false);
				if(stat==null)
					zk.create(path, "Distributed Locks's Executions".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
		} finally {
			if(zk!=null)
				zk.close();
		}
	}

	@Bean("zookeeperRootNode")
	public String zookeeperRootNode() {
		return "/"+applicationName;
	}

	@Resource(name = "zookeeperRootNode")
	private String zookeeperRootNode;

	@Bean("zookeeperSynchronizationNode")
	public String zookeeperSynchronizationNode() {
		return zookeeperRootNode+"/"+ContextConstants.SYN_NODE;
	}

	@Resource(name = "zookeeperSynchronizationNode")
	private String zookeeperSynchronizationNode;

	@Override
	public String getRootPath() {
		return zookeeperRootNode;
	}

	@Override
	public String getSynPath() {
		return zookeeperSynchronizationNode;
	}

	@PreDestroy
	public void destroy() {
		if(zooKeeperSource!=null)
			zooKeeperSource.close();
	}
}