package com.quincy.core.zookeeper;

import java.io.IOException;

import org.apache.zookeeper.ZooKeeper;

public interface ZooKeeperFactory {
	public ZooKeeper connect() throws IOException;
}