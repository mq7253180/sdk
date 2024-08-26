package com.quincy.core.aspect;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;

import com.quincy.sdk.AbstractAliveThread;
import com.quincy.sdk.annotation.Synchronized;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPubSub;

@Slf4j
@Order(4)
@Aspect
@Component
public class SynchronizedAop extends JedisNeededBaseAop<Synchronized> {
	private final static String KEY_PREFIX = "synchronization:";
	private final static String LOCK_KEY_PREFIX = KEY_PREFIX+"lock:";
	private final static String TOPIC_KEY_PREFIX = KEY_PREFIX+"topic:";
	private final static String LOCK_MAP_KEY = "lockKey";
	private final static String TOPIC_MAP_KEY = "topicKey:";
	private final static String WATCH_DOG_KEY = "watchDog";
	@Value("${spring.redis.key.prefix}")
	private String keyPrefix;

	@Pointcut("@annotation(com.quincy.sdk.annotation.Synchronized)")
    public void pointCut() {}

	@Override
	protected Class<Synchronized> annotationType() {
		return Synchronized.class;
	}

	@Override
	protected Object before(Jedis jedis, Synchronized annotation) throws NoSuchMethodException, SecurityException, InterruptedException, UnknownHostException {
		return this.tryLock(annotation.value(), jedis);
	}

	protected Map<String, ?> tryLock(String name, Jedis jedis) throws UnknownHostException {
		String lockKey = keyPrefix+LOCK_KEY_PREFIX+name;
		String topicKey = keyPrefix+TOPIC_KEY_PREFIX+name;
		String value = InetAddress.getLocalHost().getHostAddress()+"-"+Thread.currentThread().threadId();
		String cachedValue = jedis.get(lockKey);
		Map<String, ?> passToUnlock = null;
		if(cachedValue==null||cachedValue.equals("nil")) {//The lock is free to hold.
			passToUnlock = this.acquire(jedis, lockKey, value, topicKey, null);
		} else if(!cachedValue.equals(value)) {//The lock has been occupied.
			Monitor monitor = this.wait(jedis, topicKey);
			passToUnlock = this.acquire(jedis, lockKey, value, topicKey, monitor);
		}
		return passToUnlock;
	}

	private Map<String, ?> acquire(Jedis jedis, String lockKey, String value, String topicKey, Monitor _monitor) {
		Monitor monitor = _monitor;
		for(;;) {
			if(jedis.setnx(lockKey, value)==0) {//Failed then block.
				if(monitor==null) {
					monitor = this.wait(jedis, topicKey);
				} else
					this.wait(jedis, topicKey, monitor);
			} else {//Successfully hold the global lock.
				if(monitor!=null)
					monitor.cancel();
				jedis.expire(lockKey, 4);
				Thread watchDog = new WatchDog(jedis, lockKey, Thread.currentThread().threadId());
				watchDog.setDaemon(true);
				watchDog.start();
				Map<String, Object> passToUnlock = new HashMap<>(4);
				passToUnlock.put(LOCK_MAP_KEY, lockKey);
				passToUnlock.put(TOPIC_MAP_KEY, topicKey);
				passToUnlock.put(WATCH_DOG_KEY, watchDog);
				return passToUnlock;
			}
		}
	}

	@Override
	protected void after(Jedis jedis, Object passFromBefore) {
		@SuppressWarnings("unchecked")
		Map<String, ?> passFromLock = (Map<String, ?>)passFromBefore;
		this.unlock(passFromLock, jedis);
	}

	protected void unlock(Map<String, ?> passFromLock, Jedis jedis) {
		if(passFromLock!=null) {
			WatchDog watchDog = (WatchDog)passFromLock.get(WATCH_DOG_KEY);
			watchDog.cancel();
			jedis.del(passFromLock.get(LOCK_MAP_KEY).toString());
			jedis.publish(passFromLock.get(TOPIC_MAP_KEY).toString(), "Finished");
		}
	}

	private class WatchDog extends AbstractAliveThread {
		private Jedis jedis;
		private String lockKey;
		private Long currentThreadId;
		private int test = 0;

		public WatchDog(Jedis jedis, String lockKey, long currentThreadId) {
			this.jedis = jedis;
			this.lockKey = lockKey;
			this.currentThreadId = currentThreadId;
		}

		@Override
		protected void beforeLoop() {
			sleep();
		}

		@Override
		protected boolean doLoop() {
			boolean end = true;
			if(jedis.exists(lockKey)) {
				log.warn("SET_EXPIRE======{}========{}", currentThreadId, ++test);
				jedis.expire(lockKey, 4);
				sleep();
				end = false;
			}
			return end;
		}

		private void sleep() {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.error("GLOBAL_SYNC_ERROR", e);
			}
		}
	}

	private Monitor wait(Jedis jedis, String topicKey) {
		JedisPubSub listener = new JedisPubSub() {
			public void onMessage(String channel, String message) {
				this.unsubscribe();
			}
		};
		Monitor monitor = new Monitor(listener);
		monitor.setDaemon(true);
		monitor.start();
		jedis.subscribe(listener, topicKey);
		return monitor;
	}

	private void wait(Jedis jedis, String topicKey, Monitor monitor) {
		monitor.updateSubStart();
		jedis.subscribe(monitor.getListener(), topicKey);
	}

	private class Monitor extends AbstractAliveThread {
		private final static long INTERVAL = 3000;
		private JedisPubSub listener;
		private Long subStart;
		private long millis = INTERVAL;

		@Override
		protected void beforeLoop() {
			
		}

		@Override
		protected boolean doLoop() {
			try {
				log.info("{}**********SLEEP******************{}", Thread.currentThread().threadId(), millis);
				sleep(millis);
			} catch (InterruptedException e) {
				log.error("GLOBAL_SYNC_ERROR", e);
			}
			if(this.listener.isSubscribed()) {
				long fromStart = System.currentTimeMillis()-subStart;
				millis = INTERVAL-fromStart;
				log.info("{}==========NEED_SLEEP================={}", Thread.currentThread().threadId(), millis);
				if(millis<=0) {
					log.info("{}-----------------UNSUB", Thread.currentThread().threadId());
					this.listener.unsubscribe();
					millis = INTERVAL;
				}
			}
			return false;
		}

		public Monitor(JedisPubSub listener) {
			this.listener = listener;
			this.subStart = System.currentTimeMillis();
		}

		public void updateSubStart() {
			this.subStart = System.currentTimeMillis();
		}

		public JedisPubSub getListener() {
			return listener;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		JedisPubSub listener = new JedisPubSub() {
			public void onMessage(String channel, String message) {
				System.out.println("RECIEVED--------"+message);
//				this.unsubscribe();
			}
		};
//		GenericObjectPoolConfig poolParams = new GenericObjectPoolConfig();
//		poolParams.setMaxTotal(200);
//		poolParams.setMaxIdle(100);
//		poolParams.setMinIdle(50);
//		Pool<Jedis> pool = new JedisPool(poolParams, "47.93.89.0", 6379, 10, "foobared");
//		Jedis jedis = pool.getResource();
		JedisClientConfig config = new JedisClientConfig() {
			public String getPassword() {
				return "foobared";
			}
		};
		Jedis jedis = new Jedis("47.93.89.0", 6379, config);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				listener.unsubscribe();
			}
		});
		t.setDaemon(true);
//		t.start();
//		jedis.subscribe(listener, "aaa");
		jedis.set("bbb".getBytes(), "bbbb".getBytes());
		System.out.println("===========XXXX");
		jedis.close();
//		pool.close();
	}
}