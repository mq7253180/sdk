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

	@Bean
	public ZooKeeperSource zkeeperSource() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		String url = properties.getProperty("zk.url");
	    int timeout = Integer.parseInt(properties.getProperty("zk.timeout"));
	    Class<?> clazz = Class.forName(properties.getProperty("zk.watcher"));
		GenericObjectPoolConfig<PoolableZooKeeper> pc = new GenericObjectPoolConfig<PoolableZooKeeper>();
		pc.setMaxTotal(Integer.parseInt(properties.getProperty("zk.pool.maxTotal")));
		pc.setMaxIdle(Integer.parseInt(properties.getProperty("zk.pool.maxIdle")));
		pc.setMinIdle(Integer.parseInt(properties.getProperty("zk.pool.minIdle")));
		pc.setMaxWaitMillis(Long.parseLong(properties.getProperty("zk.pool.maxWaitMillis")));
		pc.setMinEvictableIdleTimeMillis(Long.parseLong(properties.getProperty("zk.pool.minEvictableIdleTimeMillis")));
		pc.setTimeBetweenEvictionRunsMillis(Long.parseLong(properties.getProperty("zk.pool.timeBetweenEvictionRunsMillis")));
		pc.setNumTestsPerEvictionRun(Integer.parseInt(properties.getProperty("zk.pool.numTestsPerEvictionRun")));
		pc.setBlockWhenExhausted(Boolean.parseBoolean(properties.getProperty("zk.pool.blockWhenExhausted")));
		pc.setTestOnBorrow(Boolean.parseBoolean(properties.getProperty("zk.pool.testOnBorrow")));
		pc.setTestWhileIdle(Boolean.parseBoolean(properties.getProperty("zk.pool.testWhileIdle")));
		pc.setTestOnReturn(Boolean.parseBoolean(properties.getProperty("zk.pool.testOnReturn")));
		pc.setEvictionPolicyClassName(MyEvictionPolicy.class.getName());
		PoolableZooKeeperFactory f = new PoolableZooKeeperFactory(url, timeout, (Watcher)clazz.newInstance());
		AbandonedConfig ac = new AbandonedConfig();
		ac.setRemoveAbandonedOnMaintenance(Boolean.parseBoolean(properties.getProperty("zk.pool.removeAbandonedOnMaintenance")));//在Maintenance的时候检查是否有泄漏
		ac.setRemoveAbandonedOnBorrow(Boolean.parseBoolean(properties.getProperty("zk.pool.removeAbandonedOnBorrow")));//borrow的时候检查泄漏
		ac.setRemoveAbandonedTimeout(Integer.parseInt(properties.getProperty("zk.pool.removeAbandonedTimeout")));//如果一个对象borrow之后n秒还没有返还给pool，认为是泄漏的对象
		ac.setLogAbandoned(Boolean.parseBoolean(properties.getProperty("zk.pool.logAbandoned")));
		ac.setUseUsageTracking(Boolean.parseBoolean(properties.getProperty("zk.pool.useUsageTracking")));
		ac.setRequireFullStackTrace(Boolean.parseBoolean(properties.getProperty("zk.pool.requireFullStackTrace")));
//		ac.setLogWriter(logWriter);
		ZooKeeperSource s = new ZooKeeperSourceImpl(f, pc, ac);
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