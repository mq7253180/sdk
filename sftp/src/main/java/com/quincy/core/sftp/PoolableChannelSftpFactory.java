package com.quincy.core.sftp;

import java.io.File;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PoolableChannelSftpFactory implements PooledObjectFactory<PoolableChannelSftp> {
	private String hostMaster;
	private String hostSlave;
	private int port;
	private String username;
	private volatile ObjectPool<PoolableChannelSftp> pool;

	public PoolableChannelSftpFactory(String hostMaster, String hostSlave, int port, String username) {
		this.hostMaster = hostMaster;
		this.hostSlave = hostSlave;
		this.port = port;
		this.username = username;
	}

	@Override
	public PooledObject<PoolableChannelSftp> makeObject() throws Exception {
		long start = System.currentTimeMillis();
		Session session = null;
		ChannelSftp channel = null;
		String dbsHost = null;
		String privateKey = "";
        if(File.separator.equals("/")) {//Linux
        		privateKey = System.getProperty("user.home") + "/.ssh/id_rsa";
        } else {//windows
        		privateKey = System.getProperty("user.home") + "\\.ssh\\wsh\\id_rsa";
        }
        JSch jsch = new JSch();
        jsch.addIdentity(privateKey);
        try {
        		dbsHost = hostMaster;
        		session = jsch.getSession(username, dbsHost, port);
	        session.setConfig("StrictHostKeyChecking", "no");
	        log.info("Connecting to remote server: {}@{} ...", username, dbsHost);
	        session.connect();
	        channel = (ChannelSftp)session.openChannel("sftp");
	        channel.connect();
        } catch(Exception e) {
        		log.error("SFTP_CONNECT_ERR: "+dbsHost+"\r\n", e);
        		if(channel!=null)
        			channel.disconnect();
        		if(session!=null)
        			session.disconnect();
        		dbsHost = hostSlave;
        		session = jsch.getSession(username, dbsHost, port);
	        session.setConfig("StrictHostKeyChecking", "no");
	        log.info("Connecting to remote server: {}@{} ...", username, dbsHost);
	        session.connect();
	        channel = (ChannelSftp)session.openChannel("sftp");
	        channel.connect();
        }
        log.info("\r\nSFTP_CONNECTING_DURATION================{}", (System.currentTimeMillis()-start));
		PoolableChannelSftp c = new PoolableChannelSftp(channel, pool);
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