package com.quincy.core.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

public class MyJedis extends Jedis {
	private JedisCluster jedisCluster;

	public MyJedis(JedisCluster jedisCluster) {
		this.jedisCluster = jedisCluster;
	}

	
}
