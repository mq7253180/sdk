package com.quincy.core.zookeeper;

import org.apache.zookeeper.ZooKeeper;

public interface ZooKeeperSource {
	public ZooKeeper get() throws Exception;
	public void close();
}