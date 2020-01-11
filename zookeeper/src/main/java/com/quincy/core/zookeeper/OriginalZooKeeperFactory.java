package com.quincy.core.zookeeper;

import java.io.IOException;

import org.apache.zookeeper.ZooKeeper;

public interface OriginalZooKeeperFactory {
	public ZooKeeper connect() throws IOException;
}