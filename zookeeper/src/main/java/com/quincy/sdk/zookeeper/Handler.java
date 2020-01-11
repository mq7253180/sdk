package com.quincy.sdk.zookeeper;

import org.apache.zookeeper.WatchedEvent;

public interface Handler {
	public String getPath();
	public void process(WatchedEvent event);
}
