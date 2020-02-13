package com.quincy.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.redis.JedisSource;
import com.quincy.core.redis.MyJedis;
import com.quincy.sdk.Constants;
import com.quincy.sdk.helper.CommonHelper;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.util.Pool;

@Configuration
public class RedisApplicationContext {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	private static Pool<Jedis> pool;
	private static Jedis jedisCluster;

	@Bean
    public JedisSource jedisPool() {
//		int redisMaxActive = Integer.parseInt(properties.getProperty("spring.redis.pool.max-active"));
		long redisMaxWait = Long.parseLong(properties.getProperty("spring.redis.pool.max-wait"));
		int redisMaxTotal = Integer.parseInt(properties.getProperty("spring.redis.pool.max-total"));
		int redisMaxIdle = Integer.parseInt(properties.getProperty("spring.redis.pool.max-idle"));
		int redisMinIdle = Integer.parseInt(properties.getProperty("spring.redis.pool.min-idle"));
		GenericObjectPoolConfig<JedisCommands> cfg = new GenericObjectPoolConfig<JedisCommands>();
		cfg.setMaxTotal(redisMaxTotal);
		cfg.setMaxIdle(redisMaxIdle);
		cfg.setMinIdle(redisMinIdle);
		cfg.setMaxWaitMillis(redisMaxWait);
		int redisTimeout = Integer.parseInt(properties.getProperty("spring.redis.timeout"));
		String redisPwd = properties.getProperty("spring.redis.password");
		String _clusterNodes = CommonHelper.trim(properties.getProperty("spring.redis.nodes"));
		if(_clusterNodes!=null) {
			Set<String> clusterNodes = new HashSet<String>(Arrays.asList(_clusterNodes.split(",")));
			String sentinelMaster = CommonHelper.trim(properties.getProperty("spring.redis.sentinel.master"));
			if(sentinelMaster!=null) {//哨兵
				pool = new JedisSentinelPool(sentinelMaster, clusterNodes, cfg, redisTimeout, redisPwd);
			} else {//集群
				Set<HostAndPort> clusterNodes_ = new HashSet<HostAndPort>(clusterNodes.size());
				for(String node:clusterNodes) {
					String[] ss = node.split(":");
					clusterNodes_.add(new HostAndPort(ss[0], Integer.valueOf(ss[1])));
				}
				jedisCluster = new MyJedis(new JedisCluster(clusterNodes_, redisTimeout, cfg));
				return new JedisSource() {
					@Override
					public Jedis get() {
						return jedisCluster;
					}
				};
			}
		} else {//单机
			String redisHost = properties.getProperty("spring.redis.host");
			int redisPort = Integer.parseInt(properties.getProperty("spring.redis.port"));
			pool = new JedisPool(cfg, redisHost, redisPort, redisTimeout, redisPwd);
		}
		return new JedisSource() {
			@Override
			public Jedis get() {
				return pool.getResource();
			}
		};
	}

	@Bean("cacheKeyPrefix")
	public String cacheKeyPrefix() {
		return properties.getProperty("spring.application.name")+".cache.";
	}

	@PreDestroy
	private void destroy() {
		if(pool!=null)
			pool.close();
		if(jedisCluster!=null)
			jedisCluster.close();
	}
}