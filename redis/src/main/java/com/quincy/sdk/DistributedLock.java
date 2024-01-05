package com.quincy.sdk;

import java.net.UnknownHostException;
import java.util.Map;

import redis.clients.jedis.Jedis;

public interface DistributedLock {
	public Map<String, ?> lock(Jedis jedis, String name) throws UnknownHostException;
	public void unlock(Jedis jedis, Map<String, ?> passFromLock);
}