package com.quincy.core.aspect;

import java.net.UnknownHostException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.quincy.core.redis.JedisSource;
import com.quincy.core.redis.RedisConstants;
import com.quincy.sdk.DistributedLock;

import redis.clients.jedis.Jedis;

@Component
public class DistributedLockImpl extends SynchronizedAop implements DistributedLock {
	@Autowired
	@Qualifier(RedisConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
	private JedisSource jedisSource;
	private final static ThreadLocal<Jedis> jedisHolder = new ThreadLocal<Jedis>();
	private final static ThreadLocal<Map<String, ?>> holder = new ThreadLocal<Map<String, ?>>();

	@Override
	public void lock(String name, Jedis jedis) throws UnknownHostException {
		jedisHolder.set(jedis);
		holder.set(this.tryLock(name, jedis));
	}

	@Override
	public void lock(String name) throws UnknownHostException {
		Jedis jedis = jedisSource.get();
		jedisHolder.set(jedis);
		holder.set(this.tryLock(name, jedis));
	}

	@Override
	public void unlock() {
		Jedis jedis = null;
    	try {
    		jedis = jedisHolder.get();
    		this.unlock(holder.get(), jedis);
    	} finally {
    		if(jedis!=null)
    			jedis.close();
    	}
	}
}