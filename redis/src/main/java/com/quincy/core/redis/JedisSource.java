package com.quincy.core.redis;

import redis.clients.jedis.Jedis;

public interface JedisSource {
	public Jedis get();
}
