package com.quincy.core;

import java.io.IOException;
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
import com.quincy.core.redis.QuincyJedis;
import com.quincy.sdk.Constants;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.util.Pool;

@Slf4j
@Configuration
public class RedisApplicationContext {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	private static Pool<Jedis> pool;
	private static QuincyJedis quincyJedis;
	protected static final int DEFAULT_MAX_ATTEMPTS = 5;

	@Bean
    public JedisSource jedisPool() {
//		int redisMaxActive = Integer.parseInt(properties.getProperty("spring.redis.pool.maxActive"));
		int maxTotal = Integer.parseInt(properties.getProperty("spring.redis.pool.maxTotal"));
		int maxIdle = Integer.parseInt(properties.getProperty("spring.redis.pool.maxIdle"));
		int minIdle = Integer.parseInt(properties.getProperty("spring.redis.pool.minIdle"));
		long maxWaitMillis = Long.parseLong(properties.getProperty("spring.redis.pool.maxWait"));
		long minEvictableIdleTimeMillis = Long.parseLong(properties.getProperty("spring.redis.pool.minEvictableIdleTimeMillis"));
		long timeBetweenEvictionRunsMillis = Long.parseLong(properties.getProperty("spring.redis.pool.timeBetweenEvictionRunsMillis"));
		int numTestsPerEvictionRun = Integer.parseInt(properties.getProperty("spring.redis.pool.numTestsPerEvictionRun"));
		boolean blockWhenExhausted = Boolean.parseBoolean(properties.getProperty("spring.redis.pool.blockWhenExhausted"));
		boolean testOnBorrow = Boolean.parseBoolean(properties.getProperty("spring.redis.pool.testOnBorrow"));
		boolean testWhileIdle = Boolean.parseBoolean(properties.getProperty("spring.redis.pool.testWhileIdle"));
		boolean testOnReturn = Boolean.parseBoolean(properties.getProperty("spring.redis.pool.testOnReturn"));
		GenericObjectPoolConfig<JedisCommands> cfg = new GenericObjectPoolConfig<JedisCommands>();
		cfg.setMaxTotal(maxTotal);
		cfg.setMaxIdle(maxIdle);
		cfg.setMinIdle(minIdle);
		cfg.setMaxWaitMillis(maxWaitMillis);
		cfg.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		cfg.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
		cfg.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
		cfg.setBlockWhenExhausted(blockWhenExhausted);
		cfg.setTestOnBorrow(testOnBorrow);
		cfg.setTestWhileIdle(testWhileIdle);
		cfg.setTestOnReturn(testOnReturn);
		int redisTimeout = Integer.parseInt(properties.getProperty("spring.redis.timeout"));
		String redisPwd = properties.getProperty("spring.redis.password");
		String _clusterNodesStr = CommonHelper.trim(properties.getProperty("spring.redis.nodes"));
		String[] _clusterNodes = _clusterNodesStr.split(",");
		log.info("REDIS_NODES======================{}", _clusterNodesStr);
		if(_clusterNodes.length>1) {
			Set<String> clusterNodes = new HashSet<String>(Arrays.asList(_clusterNodes));
			String sentinelMaster = CommonHelper.trim(properties.getProperty("spring.redis.sentinel.master"));
			if(sentinelMaster!=null) {//哨兵
				pool = new JedisSentinelPool(sentinelMaster, clusterNodes, cfg, redisTimeout, redisPwd);
				log.info("REDIS_MODE============SENTINEL");
			} else {//集群
				int soTimeout = Integer.parseInt(properties.getProperty("spring.redis.soTimeout"));
				Set<HostAndPort> clusterNodes_ = new HashSet<HostAndPort>(clusterNodes.size());
				for(String node:clusterNodes) {
					String[] ss = node.split(":");
					clusterNodes_.add(new HostAndPort(ss[0], Integer.valueOf(ss[1])));
				}
				quincyJedis = new QuincyJedis(new JedisCluster(clusterNodes_, redisTimeout, soTimeout, DEFAULT_MAX_ATTEMPTS, redisPwd, cfg));
				log.info("REDIS_MODE============CLUSTER");
				return new JedisSource() {
					@Override
					public Jedis get() {
						return quincyJedis;
					}
				};
			}
		} else {//单机
			String[] ss = _clusterNodes[0].split(":");
			String redisHost = ss[0];
			int redisPort = Integer.parseInt(ss[1]);
			pool = new JedisPool(cfg, redisHost, redisPort, redisTimeout, redisPwd);
			log.info("REDIS_MODE============SINGLETON");
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
	private void destroy() throws IOException {
		if(pool!=null)
			pool.close();
		if(quincyJedis!=null) {
			JedisCluster jedisCluster = quincyJedis.getJedisCluster();
			if(jedisCluster!=null)
				jedisCluster.close();
		}
	}
}