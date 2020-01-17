package com.quincy.core;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
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
import com.quincy.sdk.zookeeper.Handler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ZooKeeperApplicationContext implements Watcher, Context {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;
	@Value("#{'${zk.distributed-lock.keys}'.split(',')}")
	private String[] distributedLockKeys;
	private final static Map<String, Handler> handlers = new ConcurrentHashMap<String, Handler>();

	@Bean
	public ZooKeeperSource zkeeperSource() {
		String url = properties.getProperty("zk.url");
	    int timeout = Integer.parseInt(properties.getProperty("zk.timeout"));
	    Integer maxTotal = Integer.valueOf(properties.getProperty("zk.pool.max-total"));
	    Integer maxIdle = Integer.valueOf(properties.getProperty("zk.pool.max-idle"));
	    Integer minIdle = Integer.valueOf(properties.getProperty("zk.pool.min-idle"));
	    Long maxWaitMillis = Long.valueOf(properties.getProperty("zk.pool.max-wait-millis"));
	    Long softMinEvictableIdleTimeMillis = Long.valueOf(properties.getProperty("zk.pool.soft-min-evictable-idle-time-millis"));
	    Long timeBetweenEvictionRunsMillis = Long.valueOf(properties.getProperty("zk.pool.time-between-eviction-runs-millis"));
		GenericObjectPoolConfig<PoolableZooKeeper> pc = new GenericObjectPoolConfig<PoolableZooKeeper>();
		pc.setMaxIdle(maxIdle);
		pc.setMinIdle(minIdle);
		pc.setMaxTotal(maxTotal);
//		pc.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
//		pc.setTestOnCreate(true);
		pc.setTestOnBorrow(true);
//		pc.setTestOnReturn(true);
//		pc.setTestWhileIdle(true);
//		pc.setBlockWhenExhausted(true);
		pc.setMaxWaitMillis(maxWaitMillis);//最大等待时间
		pc.setMinEvictableIdleTimeMillis(-1);//最小空闲时间
		pc.setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);//最小空闲时间
		pc.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);//驱逐器触发间隔
//		pc.setEvictorShutdownTimeoutMillis(evictorShutdownTimeoutMillis);
		pc.setEvictionPolicyClassName(MyEvictionPolicy.class.getName());
		PoolableZooKeeperFactory f = new PoolableZooKeeperFactory(url, timeout, this);
		AbandonedConfig ac = new AbandonedConfig();
//		ac.setRemoveAbandonedOnMaintenance(true); //在Maintenance的时候检查是否有泄漏
//		ac.setRemoveAbandonedOnBorrow(true); //borrow 的时候检查泄漏
//		ac.setRemoveAbandonedTimeout(10);
		ZooKeeperSource s = new ZooKeeperSourceImpl(f, pc, ac);
		return s;
	}

	@Override
	public void process(WatchedEvent event) {
		log.info(event.getPath()+"==="+event.getType().name()+"==="+event.getState().name()+"==="+event.getState().getIntValue()+"==="+event.getState().ordinal()+"==="+event.toString());
		if(event.getPath()!=null) {
			Handler h = handlers.get(event.getPath());
			if(h!=null)
				h.onCreation(event);
		}
	}

	@Override
	public void addHandler(Handler h) {
		handlers.put(h.getPath(), h);
	}

	@Override
	public boolean handlerExists(String path) {
		return handlers.containsKey(path);
	}

	@Autowired
	ZooKeeperSource zooKeeperSource;

	@PostConstruct
	public void init() throws Exception {
		ZooKeeper zk = null;
		try {
			zk = zooKeeperSource.get();
			log.info("111================={}", (zk!=null));
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
			log.info("222================={}", (zk!=null));
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
}