package com.quincy.core.redis;

import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.BitOP;
import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.ClusterReset;
import redis.clients.jedis.DebugParams;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCluster.Reset;

@Slf4j
public class QuincyJedis extends Jedis {
	private final static String EXCEPTION_MSG = "The method can not be supported as cluster mode.";
	private JedisCluster jedisCluster;

	public QuincyJedis(JedisCluster jedisCluster) {
		this.jedisCluster = jedisCluster;
	}

	public JedisCluster getJedisCluster() {
		return jedisCluster;
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

	@Override
	public String clientGetname() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public byte[] clientGetnameBinary() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clientKill(final String ipPort) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clientKill(final byte[] ipPort) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clientKill(final String ip, final int port) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clientList() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public byte[] clientListBinary() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clientPause(final long timeout) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clientSetname(final String name) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clientSetname(final byte[] name) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public void close() {
		try {
			jedisCluster.close();
		} catch (IOException e) {
			log.error("JEDIS_CLUSTER_CLOSE_ERR====================", e);
		}
	}

	@Override
	public String clusterAddSlots(final int... slots) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public Long clusterCountKeysInSlot(final int slot) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterDelSlots(final int... slots) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterFailover() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterFlushSlots() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterForget(final String nodeId) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public List<String> clusterGetKeysInSlot(final int slot, final int count) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterInfo() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public Long clusterKeySlot(final String key) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterMeet(final String ip, final int port) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterNodes() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterReplicate(final String nodeId) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterReset(final Reset resetType) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterReset(final ClusterReset resetType) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterSaveConfig() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterSetSlotImporting(final int slot, final String nodeId) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterSetSlotMigrating(final int slot, final String nodeId) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterSetSlotNode(final int slot, final String nodeId) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String clusterSetSlotStable(final int slot) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public List<String> clusterSlaves(final String nodeId) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public List<Object> clusterSlots() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public List<String> configGet(final String pattern) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public List<byte[]> configGet(final byte[] pattern) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public String configResetStat() {
		return jedisCluster.configResetStat();
	}

	@Override
	public String configRewrite() {
		return jedisCluster.configRewrite();
	}

	@Override
	public String configSet(final String parameter, final String value) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public byte[] configSet(final byte[] parameter, final byte[] value) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public void connect() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public Long dbSize() {
		return jedisCluster.dbSize();
	}

	@Override
	public String debug(final DebugParams params) {
		return jedisCluster.debug(params);
	}

	@Override
	public Long decr(final String key) {
		return jedisCluster.decr(key);
	}

	@Override
	public Long decr(final byte[] key) {
		return jedisCluster.decr(key);
	}

	@Override
	public Long decrBy(final String key, final long decrement) {
		return jedisCluster.decrBy(key, decrement);
	}

	@Override
	public Long decrBy(final byte[] key, final long decrement) {
		return jedisCluster.decrBy(key, decrement);
	}

	@Override
	public Long del(final String... keys) {
		return jedisCluster.del(keys);
	}

	@Override
	public Long del(final String key) {
		return jedisCluster.del(key);
	}

	@Override
	public Long del(final byte[]... keys) {
		return jedisCluster.del(keys);
	}

	@Override
	public Long del(final byte[] key) {
		return jedisCluster.del(key);
	}

	@Override
	public void disconnect() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public byte[] dump(final String key) {
		return jedisCluster.dump(key);
	}

	@Override
	public byte[] dump(final byte[] key) {
		return jedisCluster.dump(key);
	}

	@Override
	  public String echo(final String string) {
		return jedisCluster.echo(string);
	}

	@Override
	  public byte[] echo(final byte[] string) {
		return jedisCluster.echo(string);
	}

	@Override
	  public Object eval(final String script) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	  public Object eval(final byte[] script) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	  public Object eval(final byte[] script, final byte[] keyCount, final byte[]... params) {
		return jedisCluster.eval(script, keyCount, params);
	}

	@Override
	  public Object eval(final byte[] script, final int keyCount, final byte[]... params) {
		return jedisCluster.eval(script, keyCount, params);
	}

	@Override
	  public Object eval(final byte[] script, final List<byte[]> keys, final List<byte[]> args) {
		return jedisCluster.eval(script, keys, args);
	}

	@Override
	  public Object eval(final String script, final int keyCount, final String... params) {
		return jedisCluster.eval(script, keyCount, params);
	}

	@Override
	  public Object eval(final String script, final List<String> keys, final List<String> args) {
//		Jedis jedis;
//		jedis
		return jedisCluster.eval(script, keys, args);
	}
}
