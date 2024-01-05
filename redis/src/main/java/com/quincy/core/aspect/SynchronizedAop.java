package com.quincy.core.aspect;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import com.quincy.sdk.annotation.Synchronized;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPubSub;

@Slf4j
@Aspect
@Order(2)
@Component
public class SynchronizedAop extends JedisNeededBaseAop<Synchronized> {
	private final static String KEY_PREFIX = "SYNCHRONIZATION:";
	private final static String LOCK_KEY_PREFIX = KEY_PREFIX+"LOCK:";
	private final static String TOPIC_KEY_PREFIX = KEY_PREFIX+"TOPIC:";
	private final static String LOCK_MAP_KEY = "lockKey";
	private final static String TOPIC_MAP_KEY = "topicKey:";
	private final static String WATCH_DOG_KEY = "watchDog";

	@Pointcut("@annotation(com.quincy.sdk.annotation.Synchronized)")
    public void pointCut() {}

	@Override
	protected Class<Synchronized> annotationType() {
		return Synchronized.class;
	}

	@Override
	protected Object before(Jedis jedis, Synchronized annotation) throws NoSuchMethodException, SecurityException, InterruptedException, UnknownHostException {
		String key = annotation.value();
		String lockKey = LOCK_KEY_PREFIX+key;
		String topicKey = TOPIC_KEY_PREFIX+key;
		String value = InetAddress.getLocalHost().getHostAddress()+"-"+Thread.currentThread().getId();
		String cachedValue = jedis.get(lockKey);
		Map<String, ?> passToAfter = null;
		if(cachedValue==null||cachedValue.equals("nil")) {//The lock is free to hold.
			passToAfter = this.lock(jedis, lockKey, value, topicKey, null);
		} else if(!cachedValue.equals(value)) {//The lock has been occupied.
			Monitor monitor = this.wait(jedis, topicKey);
			passToAfter = this.lock(jedis, lockKey, value, topicKey, monitor);
		}
		return passToAfter;
	}

	public void lock(Jedis jedis, String lockKey, String value, String topicKey) {
		this.lock(jedis, lockKey, value, topicKey, null);
	}

	private Map<String, ?> lock(Jedis jedis, String lockKey, String value, String topicKey, Monitor _monitor) {
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
				Thread watchDog = new WatchDog(jedis, lockKey, Thread.currentThread().getId());
				watchDog.setDaemon(true);
				watchDog.start();
				Map<String, Object> passToAfter = new HashMap<>(4);
				passToAfter.put(LOCK_MAP_KEY, lockKey);
				passToAfter.put(TOPIC_MAP_KEY, topicKey);
				passToAfter.put(WATCH_DOG_KEY, watchDog);
				return passToAfter;
			}
		}
	}

	@Override
	protected void after(Jedis jedis, Object passFromBefore) {
		if(passFromBefore!=null) {
			@SuppressWarnings("unchecked")
			Map<String, ?> keys = (Map<String, ?>)passFromBefore;
			WatchDog watchDog = (WatchDog)keys.get(WATCH_DOG_KEY);
			watchDog.cancel();
			jedis.del(keys.get(LOCK_MAP_KEY).toString());
			jedis.publish(keys.get(TOPIC_MAP_KEY).toString(), "Finished");
		}
	}

	private abstract class BaseThread extends Thread {
		protected boolean loop = true;

		public void cancel() {
			this.loop = false;
		}
	}

	private class WatchDog extends BaseThread {
		private Jedis jedis;
		private String lockKey;
		private Long currentThreadId;

		public WatchDog(Jedis jedis, String lockKey, long currentThreadId) {
			this.jedis = jedis;
			this.lockKey = lockKey;
			this.currentThreadId = currentThreadId;
		}

		@Override
		public void run() {
			sleep();
			int test = 0;
			while(loop) {
				if(jedis.exists(lockKey)) {
					log.warn("SET_EXPIRE======{}========{}", currentThreadId, ++test);
					jedis.expire(lockKey, 4);
					sleep();
				} else
					break;
			}
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

	private class Monitor extends BaseThread {
		private final static long INTERVAL = 3000;
		private JedisPubSub listener;
		private Long subStart;

		public void run() {
			long millis = INTERVAL;
			while(loop) {
				try {
					log.info("{}**********SLEEP******************{}", Thread.currentThread().getId(), millis);
					sleep(millis);
				} catch (InterruptedException e) {
					log.error("GLOBAL_SYNC_ERROR", e);
				}
				if(this.listener.isSubscribed()) {
					long fromStart = System.currentTimeMillis()-subStart;
					millis = INTERVAL-fromStart;
					log.info("{}==========NEED_SLEEP================={}", Thread.currentThread().getId(), millis);
					if(millis<=0) {
						log.info("{}-----------------UNSUB", Thread.currentThread().getId());
						this.listener.unsubscribe();
						millis = INTERVAL;
					}
				}
			}
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