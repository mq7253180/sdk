package com.quincy.core;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.zookeeper.ContextConstants;
import com.quincy.core.zookeeper.OriginalZooKeeperFactory;
import com.quincy.sdk.Constants;
import com.quincy.sdk.zookeeper.Context;
import com.quincy.sdk.zookeeper.Handler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ZooKeeperApplicationContext implements Watcher, Context {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;
	@Value("#{'${zookeeper.distributed_lock.keys}'.split(',')}")
	private String[] distributedLockKeys;
	private final static Map<String, Handler> handlers = new ConcurrentHashMap<String, Handler>();

	@Bean
	public OriginalZooKeeperFactory originalZooKeeperFactory() {
		return new OriginalZooKeeperFactory() {
			@Override
			public ZooKeeper connect() throws IOException {
				return createZooKeeper();
			}
		};
	}

	private ZooKeeper createZooKeeper() throws IOException {
		String zkUrl = properties.getProperty("zk.url");
		Integer zkTimeout = Integer.valueOf(properties.getProperty("zk.timeout"));
		ZooKeeper zk = new ZooKeeper(zkUrl, zkTimeout, this);
		return zk;
	}

	@Override
	public void process(WatchedEvent event) {
		log.info(event.getPath()+"==="+event.getType().name()+"==="+event.getState().name()+"==="+event.getState().getIntValue()+"==="+event.getState().ordinal()+"==="+event.toString());
		if(event.getPath()!=null) {
			Handler h = handlers.get(event.getPath());
			if(h!=null)
				h.process(event);
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

	@PostConstruct
	public void init() throws IOException, KeeperException, InterruptedException {
		ZooKeeper zk = null;
		try {
			zk = createZooKeeper();
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