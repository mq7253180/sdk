package com.quincy.core.zookeeper.impl;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.SwallowedExceptionListener;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;

import com.quincy.core.zookeeper.PoolableZooKeeper;
import com.quincy.core.zookeeper.PoolableZooKeeperFactory;
import com.quincy.core.zookeeper.ZooKeeperSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZooKeeperSourceImpl implements ZooKeeperSource {
	private ObjectPool<PoolableZooKeeper> pool;

	public ZooKeeperSourceImpl(PoolableZooKeeperFactory f, GenericObjectPoolConfig pc, AbandonedConfig ac) {
		GenericObjectPool<PoolableZooKeeper> pool = new GenericObjectPool<PoolableZooKeeper>(f, pc, ac);
		pool.setSwallowedExceptionListener(new SwallowedExceptionListener() {
			@Override
			public void onSwallowException(Exception e) {
				log.error("\r\nSFTP_CONNECTION_POOL_ERR:\r\n", e);
			}
		});
		f.setPool(pool);
		this.pool = pool;
	}

	@Override
	public ZooKeeper get() throws Exception {
		PoolableZooKeeper zk = null;
		while(true) {
			log.info("ZK_POOL=================NumActive: {}---NumIdle: {}", pool.getNumActive(), pool.getNumIdle());
			zk = pool.borrowObject();
			States states = zk.getState();
			if(!states.isAlive()) {
				zk.reallyClose();
				pool.invalidateObject(zk);
			} else
				return zk;
		}
	}

	@Override
	public void close() {
		this.pool.close();
	}
}