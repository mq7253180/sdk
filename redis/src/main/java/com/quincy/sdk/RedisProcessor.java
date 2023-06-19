package com.quincy.sdk;

import redis.clients.jedis.Jedis;

public interface RedisProcessor {
	public String setAndExpire(String key, String val, int expireSeconds, int retries, long retryIntervalMillis, Jedis jedis);
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds, int retries, long retryIntervalMillis, Jedis jedis);
	public String setAndExpire(String key, String val, int expireSeconds, int retries, long retryIntervalMillis);
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds, int retries, long retryIntervalMillis);
	public String setAndExpire(String key, String val, int expireSeconds, Jedis jedis);
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds, Jedis jedis);
	public String setAndExpire(String key, String val, int expireSeconds);
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds);
}