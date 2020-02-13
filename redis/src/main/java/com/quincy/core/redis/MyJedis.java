package com.quincy.core.redis;

import java.util.List;

import redis.clients.jedis.BitOP;
import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

public class MyJedis extends Jedis {
	private final static String EXCEPTION_MSG = "The method can not be supported as cluster mode.";
	private JedisCluster jedisCluster;

	public MyJedis(JedisCluster jedisCluster) {
//		Jedis jedis;
//		jedis
		this.jedisCluster = jedisCluster;
	}

	@Override
	public String asking() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String auth(final String password) {
		return jedisCluster.auth(password);
	}

	@Override
	public Long append(final String key, final String value) {
		return jedisCluster.append(key, value);
	}

	@Override
	public Long append(final byte[] key, final byte[] value) {
		return jedisCluster.append(key, value);
	}

	@Override
	public String bgrewriteaof() {
		return jedisCluster.bgrewriteaof();
	}

	@Override
	public String bgsave() {
		return jedisCluster.bgsave();
	}

	@Override
	public Long bitcount(final String key) {
		return jedisCluster.bitcount(key);
	}

	@Override
	public Long bitcount(final byte[] key) {
		return jedisCluster.bitcount(key);
	}

	@Override
	public Long bitcount(final String key, final long start, final long end) {
		return jedisCluster.bitcount(key, start, end);
	}

	@Override
	public Long bitcount(final byte[] key, final long start, final long end) {
		return jedisCluster.bitcount(key, start, end);
	}

	@Override
	public List<Long> bitfield(final String key, final String...arguments) {
		return jedisCluster.bitfield(key, arguments);
	}

	@Override
	public List<Long> bitfield(final byte[] key, final byte[]...arguments) {
		return jedisCluster.bitfield(key, arguments);
	}

	@Override
	public Long bitop(final BitOP op, final String destKey, final String... srcKeys) {
		return jedisCluster.bitop(op, destKey, srcKeys);
	}

	@Override
	public Long bitop(final BitOP op, final byte[] destKey, final byte[]... srcKeys) {
		return jedisCluster.bitop(op, destKey, srcKeys);
	}

	@Override
	public Long bitpos(final String key, final boolean value) {
		return jedisCluster.bitpos(key, value);
	}

	@Override
	public Long bitpos(final byte[] key, final boolean value) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public Long bitpos(final String key, final boolean value, final BitPosParams params) {
		return jedisCluster.bitpos(key, value, params);
	}

	@Override
	public Long bitpos(final byte[] key, final boolean value, final BitPosParams params) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public List<byte[]> blpop(final int timeout, final byte[]... keys) {
		return jedisCluster.blpop(timeout, keys);
	}

	@Override
	public List<String> blpop(final int timeout, final String key) {
		return jedisCluster.blpop(timeout, key);
	}

	@Override
	public List<String> blpop(final int timeout, final String... keys) {
		return jedisCluster.blpop(timeout, keys);
	}

	@Override
	public List<String> blpop(final String key) {
		return jedisCluster.blpop(key);
	}

	@Override
	public List<byte[]> blpop(final byte[] keys) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public List<String> blpop(final String... keys) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public List<byte[]> blpop(final byte[]... keys) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public List<byte[]> brpop(final int timeout, final byte[]... keys) {
		return jedisCluster.brpop(timeout, keys);
	}

	@Override
	public List<String> brpop(final int timeout, final String key) {
		return jedisCluster.brpop(timeout, key);
	}

	@Override
	public List<String> brpop(final int timeout, final String... keys) {
		return jedisCluster.brpop(timeout, keys);
	}

	@Override
	public List<String> brpop(final String key) {
		return jedisCluster.brpop(key);
	}

	@Override
	public List<byte[]> brpop(final byte[] keys) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public List<String> brpop(final String... keys) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public List<byte[]> brpop(final byte[]... keys) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String brpoplpush(final String source, final String destination, final int timeout) {
		return jedisCluster.brpoplpush(source, destination, timeout);
	}

	@Override
	public byte[] brpoplpush(final byte[] source, final byte[] destination, final int timeout) {
		return jedisCluster.brpoplpush(source, destination, timeout);
	}
}
