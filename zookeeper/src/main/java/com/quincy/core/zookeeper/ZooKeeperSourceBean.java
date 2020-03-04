package com.quincy.core.zookeeper;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.zookeeper.Watcher;
import org.springframework.beans.factory.FactoryBean;

import com.quincy.core.zookeeper.impl.ZooKeeperSourceImpl;

public class ZooKeeperSourceBean implements FactoryBean<ZooKeeperSource> {
	private String url;
	private int timeout;
	private String watcher;
	private GenericObjectPoolConfig poolCfg;
	private AbandonedConfig abandonedCfg;

	public ZooKeeperSourceBean(String url, int timeout, String watcher, GenericObjectPoolConfig poolCfg, AbandonedConfig abandonedCfg) {
		this.url = url;
		this.timeout = timeout;
		this.watcher = watcher;
		this.poolCfg = poolCfg;
		this.abandonedCfg = abandonedCfg;
	}

	@Override
	public ZooKeeperSource getObject() throws Exception {
		Class<?> clazz = Class.forName(watcher);
		PoolableZooKeeperFactory f = new PoolableZooKeeperFactory(url, timeout, (Watcher)clazz.newInstance());
		return new ZooKeeperSourceImpl(f, poolCfg, abandonedCfg);
	}

	@Override
	public Class<ZooKeeperSource> getObjectType() {
		return ZooKeeperSource.class;
	}
}