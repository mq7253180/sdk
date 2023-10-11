package com.quincy.core.aspect;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;

import com.quincy.sdk.annotation.Synchronized;
import com.quincy.sdk.helper.AopHelper;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
//import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
//import redis.clients.jedis.util.Pool;

@Slf4j
@Aspect
@Order(2)
@Component
public class SynchronizedAop extends JedisNeededBaseAop {
	private final static String KEY_PREFIX = "SYNCHRONIZATION:";
	private final static String LOCK_KEY_PREFIX = KEY_PREFIX+"LOCK:";
	private final static String TOPIC_KEY_PREFIX = KEY_PREFIX+"TOPIC:";
	private final static String LOCK_MAP_KEY = "lockKey";
	private final static String TOPIC_MAP_KEY = "topicKey:";
	private final static Map<JedisPubSub, MyListener> LISTENERS = new ConcurrentHashMap<>();

	@Pointcut("@annotation(com.quincy.sdk.annotation.Synchronized)")
    public void pointCut() {}

	@Override
	protected Object before(JoinPoint joinPoint, Jedis jedis) throws NoSuchMethodException, SecurityException, InterruptedException, UnknownHostException {
		Synchronized annotation = AopHelper.getAnnotation(joinPoint, Synchronized.class);
		String key = annotation.value();
		String lockKey = LOCK_KEY_PREFIX+key;
		String topicKey = TOPIC_KEY_PREFIX+key;
		String value = InetAddress.getLocalHost().getHostAddress()+"-"+Thread.currentThread().getId();
		String cachedValue = jedis.get(lockKey);
		Map<String, String> keys = null;
		if(cachedValue==null||cachedValue.equals("nil")) {//The lock is free to hold.
			keys = this.tryLock(jedis, lockKey, value, topicKey);
		} else if(!cachedValue.equals(value)) {//The lock has been occupied.
			this.block(jedis, topicKey);
			keys = this.tryLock(jedis, lockKey, value, topicKey);
		}
		return keys;
	}

	private Map<String, String> tryLock(Jedis jedis, String lockKey, String value, String topicKey) {
		for (;;) {
//			if(jedis.sismember(SET_KEY, key)||jedis.sadd(SET_KEY, key)==0)
			if(jedis.setnx(lockKey, value)==0) {//Failed then block.
				this.block(jedis, topicKey);
			} else {//Successfully hold the global lock.
				jedis.expire(lockKey, 4);
				long currentThreadId = Thread.currentThread().getId();
				Thread watchDog = new Thread(new Runnable() {
					@Override
					public void run() {
						sleep();
						int test = 0;
						while(true) {
							if(jedis.exists(lockKey)) {
								log.warn("SET_EXPIRE======{}========{}", currentThreadId, ++test);
								jedis.expire(lockKey, 4);
								sleep();
							} else
								break;
						}
					}
				});
				watchDog.setDaemon(true);
				watchDog.start();
				Map<String, String> keys = new HashMap<>(2);
				keys.put(LOCK_MAP_KEY, lockKey);
				keys.put(TOPIC_MAP_KEY, topicKey);
				return keys;
			}
		}
	}

	private void block(Jedis jedis, String key) {
		JedisPubSub listener = new JedisPubSub() {
			public void onMessage(String channel, String message) {
				this.unsubscribe();
				LISTENERS.remove(this);
			}
		};
		LISTENERS.put(listener, new MyListener(listener));
		jedis.subscribe(listener, key);
	}

	private void sleep() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			log.error("GLOBAL_SYNC_ERROR", e);
		}
	}

	@Override
	protected void after(JoinPoint joinPoint, Jedis jedis, Object obj) {
		if(obj!=null) {
			@SuppressWarnings("unchecked")
			Map<String, String> keys = (Map<String, String>)obj;
//			jedis.srem(SET_KEY, key);
			jedis.del(keys.get(LOCK_MAP_KEY));
			jedis.publish(keys.get(TOPIC_MAP_KEY), "Finished");
		}
	}

	private class MyListener {
		private JedisPubSub listener;
		private long start = System.currentTimeMillis();

		public MyListener(JedisPubSub listener) {
			this.listener = listener;
		}

		public JedisPubSub getListener() {
			return listener;
		}
		public long getStart() {
			return start;
		}
	}

	@Scheduled(cron = "0/3 * * * * ?")
	public void monitor() {
		long current = System.currentTimeMillis();
		Collection<MyListener> values = LISTENERS.values();
		for(MyListener m:values) {
			if(current-m.getStart()>3000) {
				log.warn("MONITOR============{}", LISTENERS.size());
				JedisPubSub listener = m.getListener();
				listener.unsubscribe();
				LISTENERS.remove(listener);
			}
		}
	}

	public static void main(String[] args) throws InterruptedException {
		JedisPubSub listener = new JedisPubSub() {
			public void onMessage(String channel, String message) {
				System.out.println("RECIEVED--------"+message);
//				this.unsubscribe();
			}
		};
		GenericObjectPoolConfig poolParams = new GenericObjectPoolConfig();
		poolParams.setMaxTotal(200);
		poolParams.setMaxIdle(100);
		poolParams.setMinIdle(50);
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