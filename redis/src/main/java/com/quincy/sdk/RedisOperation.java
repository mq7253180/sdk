package com.quincy.sdk;

import redis.clients.jedis.Jedis;

public interface RedisOperation {
	public Object run(Jedis jedis) throws Exception;
}