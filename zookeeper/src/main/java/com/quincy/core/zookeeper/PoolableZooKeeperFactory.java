package com.quincy.core.zookeeper;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper.States;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PoolableZooKeeperFactory implements PooledObjectFactory<PoolableZooKeeper> {
	private String url;
	private int timeout;
	private Watcher watcher;
	private volatile ObjectPool<PoolableZooKeeper> pool;

	public PoolableZooKeeperFactory(String url, int timeout, Watcher watcher) {
		this.url = url;
		this.timeout = timeout;
		this.watcher = watcher;
	}

	@Override
	public PooledObject<PoolableZooKeeper> makeObject() throws Exception {
		PoolableZooKeeper pzk = new PoolableZooKeeper(url, timeout, watcher, pool);
		return new DefaultPooledObject<PoolableZooKeeper>(pzk);
	}

	@Override
	public void destroyObject(PooledObject<PoolableZooKeeper> p) throws Exception {
		log.info("ZK_POOL=======================================destroyObject");
		p.getObject().reallyClose();
	}

	@Override
	public boolean validateObject(PooledObject<PoolableZooKeeper> p) {
		log.info("ZK_POOL=======================================validateObject");
		States states = p.getObject().getState();
		try {
			log.info("ZK_POOL------------------isAlive: {}===isConnected: {}", states.isAlive(), states.isConnected());
			if(states.isAlive()) {
				log.info("ZK_POOL------------------OBJ_VALIDATION_TRUE");
				return true;
			} else {
				log.info("ZK_POOL------------------OBJ_VALIDATION_FALSE");
				return false;
			}
		} catch (final Exception e) {
			log.error("\r\nOBJECT_VALIDATION_ERR: \r\n", e);
			return false;
		}
	}

	@Override
	public void activateObject(PooledObject<PoolableZooKeeper> p) throws Exception {
		log.info("ZK_POOL=======================================activateObject");
	}

	@Override
	public void passivateObject(PooledObject<PoolableZooKeeper> p) throws Exception {
		log.info("ZK_POOL=======================================passivateObject");
	}

	public ObjectPool<PoolableZooKeeper> getPool() {
		return pool;
	}
	public void setPool(ObjectPool<PoolableZooKeeper> pool) {
		this.pool = pool;
	}
}