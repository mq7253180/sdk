package com.quincy.core.sftp.impl;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.SwallowedExceptionListener;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.jcraft.jsch.ChannelSftp;
import com.quincy.core.sftp.ChannelSftpSource;
import com.quincy.core.sftp.PoolableChannelSftp;
import com.quincy.core.sftp.PooledChannelSftpFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChannelSftpSourceImpl implements ChannelSftpSource {
	private ObjectPool<PoolableChannelSftp> pool;

	public ChannelSftpSourceImpl(PooledChannelSftpFactory f, GenericObjectPoolConfig<PoolableChannelSftp> pc, AbandonedConfig ac) {
		GenericObjectPool<PoolableChannelSftp> pool = new GenericObjectPool<PoolableChannelSftp>(f, pc, ac);
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
	public ChannelSftp get() throws Exception {
		PoolableChannelSftp channel = pool.borrowObject();
		if(channel.isClosed()||!channel.isConnected()) {
			PoolableChannelSftp poolableChannelSftp = (PoolableChannelSftp)channel;
			poolableChannelSftp.reallyDisconnect();
			pool.invalidateObject(channel);
		}
		return channel;
	}

	@Override
	public void close() {
		this.pool.close();
	}
}
