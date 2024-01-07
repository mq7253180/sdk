package com.quincy.sdk;

import java.net.UnknownHostException;

import redis.clients.jedis.Jedis;

public interface DistributedLock {
	public void lock(String name, Jedis jedis) throws UnknownHostException;
	public void lock(String name) throws UnknownHostException;
	public void unlock();
}