package com.quincy.core.redis;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPubSub;

public class QuincyJedis extends Jedis {
//	private final static String EXCEPTION_MSG = "The method can not be supported as cluster mode.";
	private JedisCluster jedisCluster;

	public QuincyJedis(JedisCluster jedisCluster) {
		this.jedisCluster = jedisCluster;
	}

	public JedisCluster getJedisCluster() {
		return jedisCluster;
	}

	@Override
	public Boolean exists(final String key) {
		return jedisCluster.exists(key);
	}

	@Override
	public Boolean exists(final byte[] key) {
		return jedisCluster.exists(key);
	}

	@Override
	public Long exists(final String... keys) {
		return jedisCluster.exists(keys);
	}

	@Override
	public Long exists(final byte[]... keys) {
		return jedisCluster.exists(keys);
	}

	@Override
	public String set(final String key, final String value) {
		return jedisCluster.set(key, value);
	}

	@Override
	public String set(final byte[] key, final byte[] value) {
		return jedisCluster.set(key, value);
	}

	@Override
	public String set(final String key, final String value, final String nxxx, final String expx,
		      final long time) {
		return jedisCluster.set(key, value, nxxx, expx, time);
	}

	@Override
	public String set(final byte[] key, final byte[] value, final byte[] nxxx, final byte[] expx,
		      final long time) {
		return jedisCluster.set(key, value, nxxx, expx, time);
	}

	@Override
	public String set(final String key, final String value, final String nxxx, final String expx,
		      final int time) {
		return jedisCluster.set(key, value, nxxx, expx, time);
	}

	@Override
	public String set(final byte[] key, final byte[] value, final byte[] nxxx, final byte[] expx,
		      final int time) {
		return jedisCluster.set(key, value, nxxx, expx, time);
	}

	@Override
	public Long setnx(final String key, final String value) {
		return jedisCluster.setnx(key, value);
	}

	@Override
	public Long setnx(final byte[] key, final byte[] value) {
		return jedisCluster.setnx(key, value);
	}

	@Override
	public String get(final String key) {
		return jedisCluster.get(key);
	}

	@Override
	public byte[] get(final byte[] key) {
		return jedisCluster.get(key);
	}

	@Override
	public Long expire(final String key, final int seconds) {
		return jedisCluster.expire(key, seconds);
	}

	@Override
	public Long expire(final byte[] key, final int seconds) {
		return jedisCluster.expire(key, seconds);
	}

	@Override
	public Long del(final String key) {
		return jedisCluster.del(key);
	}

	@Override
	public Long del(final byte[] key) {
		return jedisCluster.del(key);
	}

	@Override
	public Long sadd(final String key, final String... members) {
		return jedisCluster.sadd(key, members);
	}

	@Override
	public Long sadd(final byte[] key, final byte[]... members) {
		return jedisCluster.sadd(key, members);
	}

	@Override
	public Long srem(final String key, final String... members) {
		return jedisCluster.srem(key, members);
	}

	@Override
	public Long srem(final byte[] key, final byte[]... members) {
		return jedisCluster.srem(key, members);
	}

	@Override
	public Boolean sismember(final String key, final String member) {
		return jedisCluster.sismember(key, member);
	}

	@Override
	public Boolean sismember(final byte[] key, final byte[] member) {
		return jedisCluster.sismember(key, member);
	}

	@Override
	public Long publish(final String channel, final String message) {
		return jedisCluster.publish(channel, message);
	}

	@Override
	public Long publish(final byte[] channel, final byte[] message) {
		return jedisCluster.publish(channel, message);
	}

	@Override
	public void subscribe(final JedisPubSub jedisPubSub, final String... channels) {
		jedisCluster.subscribe(jedisPubSub, channels);
	}

	@Override
	public void subscribe(final BinaryJedisPubSub jedisPubSub, final byte[]... channels) {
		jedisCluster.subscribe(jedisPubSub, channels);
	}

	@Override
	public String hget(final String key, final String field) {
		return jedisCluster.hget(key, field);
	}

	@Override
	public byte[] hget(final byte[] key, final byte[] field) {
		return jedisCluster.hget(key, field);
	}

	@Override
	public Long hdel(final String key, final String... fields) {
		return jedisCluster.hdel(key, fields);
	}

	@Override
	public Long hdel(final byte[] key, final byte[]... fields) {
		return jedisCluster.hdel(key, fields);
	}

	@Override
	public Long hincrBy(final String key, final String field, final long value) {
		return jedisCluster.hincrBy(key, field, value);
	}

	@Override
	public Long hincrBy(final byte[] key, final byte[] field, final long value) {
		return jedisCluster.hincrBy(key, field, value);
	}

	/*@Override
	public void close() {
		try {
			jedisCluster.close();
		} catch (IOException e) {
			log.error("JEDIS_CLUSTER_CLOSE_ERR====================", e);
		}
	}*/
}