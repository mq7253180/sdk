package com.quincy.core.zookeeper;

import java.io.IOException;

import org.apache.commons.pool2.ObjectPool;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PoolableZooKeeper extends ZooKeeper {
	private volatile ObjectPool<PoolableZooKeeper> pool;

	public PoolableZooKeeper(String connectString, int sessionTimeout, Watcher watcher, long sessionId,
			byte[] sessionPasswd, boolean canBeReadOnly, ObjectPool<PoolableZooKeeper> pool) throws IOException {
		super(connectString, sessionTimeout, watcher, sessionId, sessionPasswd, canBeReadOnly);
		this.pool = pool;
	}

	public PoolableZooKeeper(String connectString, int sessionTimeout, Watcher watcher, long sessionId,
			byte[] sessionPasswd, ObjectPool<PoolableZooKeeper> pool) throws IOException {
		super(connectString, sessionTimeout, watcher, sessionId, sessionPasswd);
		this.pool = pool;
	}

	public PoolableZooKeeper(String connectString, int sessionTimeout, Watcher watcher,
            boolean canBeReadOnly, ObjectPool<PoolableZooKeeper> pool) throws IOException {
		super(connectString, sessionTimeout, watcher, canBeReadOnly);
		this.pool = pool;
	}

	public PoolableZooKeeper(String connectString, int sessionTimeout, Watcher watcher, ObjectPool<PoolableZooKeeper> pool) throws IOException {
		super(connectString, sessionTimeout, watcher);
		this.pool = pool;
	}

	public void close() {
		try {
			this.pool.returnObject(this);
		} catch (Exception e) {
			log.error("SFTP_POOL======================", e);
		}
	}

	public void reallyClose() throws InterruptedException {
		this.close();
	}
}