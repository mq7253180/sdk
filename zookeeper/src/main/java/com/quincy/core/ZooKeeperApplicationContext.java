package com.quincy.core;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.zookeeper.ContextConstants;
import com.quincy.core.zookeeper.PoolableZooKeeper;
import com.quincy.core.zookeeper.PoolableZooKeeperFactory;
import com.quincy.core.zookeeper.ZooKeeperSource;
import com.quincy.core.zookeeper.impl.ZooKeeperSourceImpl;
import com.quincy.sdk.Constants;
import com.quincy.sdk.zookeeper.Context;

@Configuration
public class ZooKeeperApplicationContext implements Context {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;
	@Value("#{'${zk.distributedLock.keys}'.split(',')}")
	private String[] distributedLockKeys;
	@Autowired
	private GenericObjectPoolConfig<?> poolCfg;
	@Autowired
	private AbandonedConfig abandonedCfg;

	@Bean
	public ZooKeeperSource zkeeperSource() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		String url = properties.getProperty("zk.url");
	    int timeout = Integer.parseInt(properties.getProperty("zk.timeout"));
	    Class<?> clazz = Class.forName(properties.getProperty("zk.watcher"));
		GenericObjectPoolConfig<PoolableZooKeeper> cfg = new GenericObjectPoolConfig<PoolableZooKeeper>();
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
		PoolableZooKeeperFactory f = new PoolableZooKeeperFactory(url, timeout, (Watcher)clazz.newInstance());
		ZooKeeperSource s = new ZooKeeperSourceImpl(f, cfg, abandonedCfg);
		return s;
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
		return "/"+properties.getProperty("spring.application.name");
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

	@Autowired
	private ZooKeeperSource zkSource;

	@PreDestroy
	public void destroy() {
		if(zkSource!=null)
			zkSource.close();
	}
}