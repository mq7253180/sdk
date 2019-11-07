package com.quincy.core.sftp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.apache.commons.pool2.ObjectPool;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

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

	public void cd(String path) throws SftpException{
		this.channelSftp.cd(path);
	}

	public boolean isConnected() {
		return this.channelSftp.isConnected();
	}

	public boolean isClosed() {
		return this.channelSftp.isClosed();
	}

	public Vector ls(String path) throws SftpException{
		return this.channelSftp.ls(path);
	}

	public void put(InputStream src, String dst) throws SftpException{
		this.channelSftp.put(src, dst);
	}

	public void get(String src, OutputStream dst) throws SftpException{
		this.channelSftp.get(src, dst);
	}
}
