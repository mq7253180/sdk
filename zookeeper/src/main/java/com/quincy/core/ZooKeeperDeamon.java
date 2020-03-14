package com.quincy.core;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.quincy.core.zookeeper.ZooKeeperSource;

@Component
public class ZooKeeperDeamon {
	@Autowired
	private ZooKeeperSource zooKeeperSource;
	@Resource(name = "zookeeperRootNode")
	private String zookeeperRootNode;
	@Resource(name = "zookeeperSynchronizationNode")
	private String zookeeperSynchronizationNode;
	@Value("#{'${zk.distributedLock.keys}'.split(',')}")
	private String[] distributedLockKeys;

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

	@PreDestroy
	public void destroy() {
		if(zooKeeperSource!=null)
			zooKeeperSource.close();
	}
}