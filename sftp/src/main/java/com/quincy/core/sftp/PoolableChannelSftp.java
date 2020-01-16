package com.quincy.core.sftp;

import org.apache.commons.pool2.ObjectPool;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;

public class PoolableChannelSftp extends ChannelSftp {
	private ChannelSftp channelSftp;
	private ObjectPool<PoolableChannelSftp> pool;

	public PoolableChannelSftp(final ChannelSftp channelSftp, final ObjectPool<PoolableChannelSftp> pool) {
		this.channelSftp = channelSftp;
		this.pool = pool;
	}

	public void disconnect() {
		try {
			this.pool.returnObject(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void reallyDisconnect() throws JSchException {
		this.channelSftp.disconnect();
		this.channelSftp.getSession().disconnect();
	}
}