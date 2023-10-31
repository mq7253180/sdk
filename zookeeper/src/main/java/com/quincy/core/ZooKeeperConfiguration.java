package com.quincy.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import com.quincy.sdk.ZKContext;

@Configuration
public class ZooKeeperConfiguration implements ZKContext {
	@Autowired
	@Qualifier("zookeeperRootNode")
	private String zookeeperRootNode;

	@Autowired
	@Qualifier("zookeeperSynchronizationNode")
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