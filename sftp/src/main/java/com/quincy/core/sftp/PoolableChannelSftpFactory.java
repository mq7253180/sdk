package com.quincy.core.sftp;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PoolableChannelSftpFactory implements PooledObjectFactory<PoolableChannelSftp> {
	private String host;
	private int port;
	private String username;
	private String privateKey;
	private volatile ObjectPool<PoolableChannelSftp> pool;

	public PoolableChannelSftpFactory(String host, int port, String username, String privateKey) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.privateKey = privateKey;
	}

	@Override
	public PooledObject<PoolableChannelSftp> makeObject() throws JSchException {
		long start = System.currentTimeMillis();
        JSch jsch = new JSch();
        jsch.addIdentity(privateKey);
        Session session = jsch.getSession(username, host, port);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        ChannelSftp channel = (ChannelSftp)session.openChannel("sftp");
        channel.connect();
		PoolableChannelSftp c = new PoolableChannelSftp(channel, pool);
		log.info("\r\nSFTP_CONNECTING_DURATION================{}", (System.currentTimeMillis()-start));
		return new DefaultPooledObject<PoolableChannelSftp>(c);
	}

	@Override
	public void destroyObject(PooledObject<PoolableChannelSftp> p) throws Exception {
		log.info("SFTP_POOL=======================================destroyObject");
		/*StackTraceElement stack[] = Thread.currentThread().getStackTrace();  
        for(int i=0;i<stack.length;i++){
        	log.info("-----"+stack[i].getClassName()+"."+stack[i].getMethodName());
        }*/
		p.getObject().reallyDisconnect();
	}

	@Override
	public boolean validateObject(PooledObject<PoolableChannelSftp> p) {
		log.info("SFTP_POOL=======================================validateObject");
		try {
			log.info("SFTP_POOL------------------isClosed: {}===isConnected: {}", p.getObject().isClosed(), p.getObject().isConnected());
			if((!p.getObject().isClosed())&&p.getObject().isConnected()) {
				log.info("SFTP_POOL------------------OBJ_VALIDATION_TRUE");
				return true;
			} else {
				log.info("SFTP_POOL------------------OBJ_VALIDATION_FALSE");
				return false;
			}
		} catch (final Exception e) {
			log.error("\r\nOBJECT_VALIDATION_ERR: \r\n", e);
			return false;
		}
	}

	@Override
	public void activateObject(PooledObject<PoolableChannelSftp> p) throws Exception {
		log.info("SFTP_POOL=======================================activateObject");
	}

	@Override
	public void passivateObject(PooledObject<PoolableChannelSftp> p) throws Exception {
		log.info("SFTP_POOL=======================================passivateObject");
	}

	public ObjectPool<PoolableChannelSftp> getPool() {
		return pool;
	}
	public void setPool(ObjectPool<PoolableChannelSftp> pool) {
		this.pool = pool;
	}
}