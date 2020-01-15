package com.quincy.core.aspect;

import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.core.zookeeper.ZooKeeperFactory;
import com.quincy.sdk.annotation.Synchronized;
import com.quincy.sdk.helper.AopHelper;
import com.quincy.sdk.zookeeper.Context;

import lombok.Data;

@Aspect
@Order(2)
@Component
public class SynchronizedAop {
	@Autowired
	private ZooKeeperFactory factory;
	@Autowired
	private Context context;
	private final static String KEY = "execution";

	@Pointcut("@annotation(com.quincy.sdk.annotation.Synchronized)")
    public void pointCut() {}

	@Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Synchronized annotation = AopHelper.getAnnotation(joinPoint, Synchronized.class);
		String key = annotation.value();
		String path = context.getSynPath()+"/"+key;
		String lockPath = path+"/"+KEY;
		String realPath = null;
		ZooKeeper zk = null;
		try {
			zk = factory.connect();
			realPath = zk.create(lockPath, KEY.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
			String seqStr = realPath.substring(realPath.indexOf(KEY)+KEY.length(), realPath.length());
			int seq = Integer.parseInt(seqStr);
			Lock lock = new Lock();
			int retried = 0;
			while(true) {
				List<String> pathes = zk.getChildren(path, new Watcher() {
					@Override
					public void process(WatchedEvent event) {
						synchronized(lock) {
							lock.setNotified(true);
							lock.notifyAll();
						}
					}
				});
				int minSeq = -1;
				for(String p:pathes) {
					int toComparedSeq = Integer.parseInt(p.substring(KEY.length(), p.length()));
					if(minSeq==-1||toComparedSeq<minSeq)
						minSeq = toComparedSeq;
				}
				if(seq>minSeq) {//没拿到锁
					synchronized(lock) {
						lock.wait(annotation.timeout());
					}
					if(!lock.isNotified()) {
						retried++;
						if(retried>=annotation.retries())
							throw new RuntimeException("Distributed Lock Timeout!");
					}
					lock.setNotified(false);
				} else//拿到锁
					break;
			}
			Object toReturn = joinPoint.proceed();
			zk.delete(realPath, -1);//Release the lock.
			return toReturn;
		} finally {
			if(zk!=null)
				zk.close();
		}
	}

	@Data
	private class Lock {
		private boolean notified = false;
	}
}