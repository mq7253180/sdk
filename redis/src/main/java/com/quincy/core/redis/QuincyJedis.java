package com.quincy.core.redis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.BitOP;
import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.Client;
import redis.clients.jedis.ClusterReset;
import redis.clients.jedis.DebugParams;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.JedisCluster.Reset;
import redis.clients.jedis.params.geo.GeoRadiusParam;

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
		return jedisCluster.eval(script, keys, args);
	}

	@Override
	public String flushAll() {
		return jedisCluster.flushAll();
	}

	@Override
	public String flushDB() {
		return jedisCluster.flushDB();
	}

	@Override
	public Long geoadd(final String key, final Map<String, GeoCoordinate> memberCoordinateMap) {
		return jedisCluster.geoadd(key, memberCoordinateMap);
	}

	@Override
	public Long geoadd(final byte[] key, final Map<byte[], GeoCoordinate> memberCoordinateMap) {
		return jedisCluster.geoadd(key, memberCoordinateMap);
	}

	@Override
	public Long geoadd(final String key, final double longitude, final double latitude, final String member) {
		return jedisCluster.geoadd(key, longitude, latitude, member);
	}

	@Override
	public Long geoadd(final byte[] key, final double longitude, final double latitude, final byte[] member) {
		return jedisCluster.geoadd(key, longitude, latitude, member);
	}

	@Override
	public Double geodist(final String key, final String member1, final String member2) {
		return jedisCluster.geodist(key, member1, member2);
	}

	@Override
	public Double geodist(final byte[] key, final byte[] member1, final byte[] member2) {
		return jedisCluster.geodist(key, member1, member2);
	}

	@Override
	public Double geodist(final String key, final String member1, final String member2, final GeoUnit unit) {
		return jedisCluster.geodist(key, member1, member2, unit);
	}

	@Override
	public Double geodist(final byte[] key, final byte[] member1, final byte[] member2, final GeoUnit unit) {
		return jedisCluster.geodist(key, member1, member2, unit);
	}

	@Override
	public List<String> geohash(final String key, String... members) {
		return jedisCluster.geohash(key, members);
	}

	@Override
	public List<byte[]> geohash(final byte[] key, byte[]... members) {
		return jedisCluster.geohash(key, members);
	}

	@Override
	public List<GeoCoordinate> geopos(final String key, String... members) {
		return jedisCluster.geopos(key, members);
	}

	@Override
	public List<GeoCoordinate> geopos(final byte[] key, byte[]... members) {
		return jedisCluster.geopos(key, members);
	}

	@Override
	public List<GeoRadiusResponse> georadius(final String key, final double longitude, final double latitude,
			final double radius, final GeoUnit unit) {
		return jedisCluster.georadius(key, longitude, latitude, radius, unit);
	}

	@Override
	public List<GeoRadiusResponse> georadius(final byte[] key, final double longitude, final double latitude,
			final double radius, final GeoUnit unit) {
		return jedisCluster.georadius(key, longitude, latitude, radius, unit);
	}

	@Override
	public List<GeoRadiusResponse> georadius(final String key, final double longitude, final double latitude,
			final double radius, final GeoUnit unit, final GeoRadiusParam param) {
		return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
	}

	@Override
	public List<GeoRadiusResponse> georadius(final byte[] key, final double longitude, final double latitude,
			final double radius, final GeoUnit unit, final GeoRadiusParam param) {
		return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMember(final String key, final String member, final double radius,
			final GeoUnit unit) {
		return jedisCluster.georadiusByMember(key, member, radius, unit);
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMember(final byte[] key, final byte[] member, final double radius,
			final GeoUnit unit) {
		return jedisCluster.georadiusByMember(key, member, radius, unit);
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMember(final String key, final String member, final double radius,
			final GeoUnit unit, final GeoRadiusParam param) {
		return jedisCluster.georadiusByMember(key, member, radius, unit, param);
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMember(final byte[] key, final byte[] member, final double radius,
			final GeoUnit unit, final GeoRadiusParam param) {
		return jedisCluster.georadiusByMember(key, member, radius, unit, param);
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMemberReadonly(final String key, final String member, final double radius,
			final GeoUnit unit) {
		return jedisCluster.georadiusByMemberReadonly(key, member, radius, unit);
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMemberReadonly(final byte[] key, final byte[] member, final double radius,
			final GeoUnit unit) {
		return jedisCluster.georadiusByMemberReadonly(key, member, radius, unit);
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMemberReadonly(final String key, final String member, final double radius,
			final GeoUnit unit, final GeoRadiusParam param) {
		return jedisCluster.georadiusByMemberReadonly(key, member, radius, unit, param);
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMemberReadonly(final byte[] key, final byte[] member, final double radius,
			final GeoUnit unit, final GeoRadiusParam param) {
		return jedisCluster.georadiusByMemberReadonly(key, member, radius, unit, param);
	}

	@Override
	public List<GeoRadiusResponse> georadiusReadonly(final String key, final double longitude, final double latitude,
			final double radius, final GeoUnit unit) {
		return jedisCluster.georadiusReadonly(key, longitude, latitude, radius, unit);
	}

	@Override
	public List<GeoRadiusResponse> georadiusReadonly(final byte[] key, final double longitude, final double latitude,
			final double radius, final GeoUnit unit) {
		return jedisCluster.georadiusReadonly(key, longitude, latitude, radius, unit);
	}

	@Override
	public List<GeoRadiusResponse> georadiusReadonly(final String key, final double longitude, final double latitude,
			final double radius, final GeoUnit unit, final GeoRadiusParam param) {
		return jedisCluster.georadiusReadonly(key, longitude, latitude, radius, unit, param);
	}

	@Override
	public List<GeoRadiusResponse> georadiusReadonly(final byte[] key, final double longitude, final double latitude,
			final double radius, final GeoUnit unit, final GeoRadiusParam param) {
		return jedisCluster.georadiusReadonly(key, longitude, latitude, radius, unit, param);
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
	public Boolean getbit(final String key, final long offset) {
		return jedisCluster.getbit(key, offset);
	}

	@Override
	public Boolean getbit(final byte[] key, final long offset) {
		return jedisCluster.getbit(key, offset);
	}

	@Override
	public Client getClient() {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public Long getDB() {
		return jedisCluster.getDB();
	}

	@Override
	public String getrange(final String key, final long startOffset, final long endOffset) {
		return jedisCluster.getrange(key, startOffset, endOffset);
	}

	@Override
	public byte[] getrange(final byte[] key, final long startOffset, final long endOffset) {
		return jedisCluster.getrange(key, startOffset, endOffset);
	}

	@Override
	public String getSet(final String key, final String value) {
		return jedisCluster.getSet(key, value);
	}

	@Override
	public byte[] getSet(final byte[] key, final byte[] value) {
		return jedisCluster.getSet(key, value);
	}

	protected static String[] getParams(List<String> keys, List<String> args) {
		return getParams(keys, args);
	}

	protected static byte[][] getParamsWithBinary(List<byte[]> keys, List<byte[]> args) {
		return getParamsWithBinary(keys, args);
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
	public Boolean hexists(final String key, final String field) {
		return jedisCluster.hexists(key, field);
	}

	@Override
	public Boolean hexists(final byte[] key, final byte[] field) {
		return jedisCluster.hexists(key, field);
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
	public Map<String, String> hgetAll(final String key) {
		return jedisCluster.hgetAll(key);
	}

	@Override
	public Map<byte[], byte[]> hgetAll(final byte[] key) {
		return jedisCluster.hgetAll(key);
	}

	@Override
	public Long hincrBy(final String key, final String field, final long value) {
		return jedisCluster.hincrBy(key, field, value);
	}

	@Override
	public Long hincrBy(final byte[] key, final byte[] field, final long value) {
		return jedisCluster.hincrBy(key, field, value);
	}

	@Override
	public Double hincrByFloat(final String key, final String field, final double value) {
		return jedisCluster.hincrByFloat(key, field, value);
	}

	@Override
	public Double hincrByFloat(final byte[] key, final byte[] field, final double value) {
		return jedisCluster.hincrByFloat(key, field, value);
	}

	@Override
	public Set<String> hkeys(final String key) {
		return jedisCluster.hkeys(key);
	}

	@Override
	public Set<byte[]> hkeys(final byte[] key) {
		return jedisCluster.hkeys(key);
	}

	@Override
	public Long hlen(final String key) {
		return jedisCluster.hlen(key);
	}

	@Override
	public Long hlen(final byte[] key) {
		return jedisCluster.hlen(key);
	}

	@Override
	public List<String> hmget(final String key, final String... fields) {
		return jedisCluster.hmget(key, fields);
	}

	@Override
	public List<byte[]> hmget(final byte[] key, final byte[]... fields) {
		return jedisCluster.hmget(key, fields);
	}

	@Override
	public String hmset(final String key, final Map<String, String> hash) {
		return jedisCluster.hmset(key, hash);
	}

	@Override
	public String hmset(final byte[] key, final Map<byte[], byte[]> hash) {
		return jedisCluster.hmset(key, hash);
	}

	@Override
	public ScanResult<Map.Entry<byte[], byte[]>> hscan(final byte[] key, final byte[] cursor) {
		return jedisCluster.hscan(key, cursor);
	}

	@Override
	public ScanResult<Map.Entry<String, String>> hscan(final String key, final String cursor) {
		return jedisCluster.hscan(key, cursor);
	}

	@Override
	public ScanResult<Map.Entry<String, String>> hscan(final String key, final int cursor) {
		return jedisCluster.hscan(key, cursor);
	}

	@Override
	public ScanResult<Map.Entry<byte[], byte[]>> hscan(final byte[] key, final byte[] cursor,
			final ScanParams params) {
		return jedisCluster.hscan(key, cursor, params);
	}

	@Override
	public ScanResult<Map.Entry<String, String>> hscan(final String key, final String cursor,
			final ScanParams params) {
		return jedisCluster.hscan(key, cursor, params);
	}

	@Override
	public ScanResult<Map.Entry<String, String>> hscan(final String key, final int cursor,
			final ScanParams params) {
		throw new RuntimeException(EXCEPTION_MSG);
	}

	@Override
	public Long hset(final String key, final Map<String, String> hash) {
		return jedisCluster.hset(key, hash);
	}

	@Override
	public Long hset(final String key, final String field, final String value) {
		return jedisCluster.hset(key, field, value);
	}

	@Override
	public Long hset(final byte[] key, final Map<byte[], byte[]> hash) {
		return jedisCluster.hset(key, hash);
	}

	@Override
	public Long hset(final byte[] key, final byte[] field, final byte[] value) {
		return jedisCluster.hset(key, field, value);
	}

	@Override
	public Long hsetnx(final String key, final String field, final String value) {
		return jedisCluster.hsetnx(key, field, value);
	}

	@Override
	public Long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
		return jedisCluster.hsetnx(key, field, value);
	}

	@Override
	public Long hstrlen(final String key, final String field) {
		return jedisCluster.hstrlen(key, field);
	}

	@Override
	public Long hstrlen(final byte[] key, final byte[] field) {
		return jedisCluster.hstrlen(key, field);
	}

	@Override
	public List<String> hvals(final String key) {
//		Jedis jedis;
//		jedis;
		return jedisCluster.hvals(key);
	}

	@Override
	public List<byte[]> hvals(final byte[] key) {
		Collection<byte[]> collection = jedisCluster.hvals(key);
		byte[][] bb = new byte[collection.size()][];
		return Arrays.asList(collection.toArray(bb));
	}
}
