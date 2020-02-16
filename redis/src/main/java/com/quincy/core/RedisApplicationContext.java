package com.quincy.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.redis.JedisSource;
import com.quincy.core.redis.QuincyJedis;
import com.quincy.sdk.Constants;
import com.quincy.sdk.PoolParams;
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
	@Autowired
	private PoolParams poolParams;

	private static Pool<Jedis> pool;
	private static QuincyJedis quincyJedis;
	protected static final int DEFAULT_MAX_ATTEMPTS = 5;

	@Bean
    public JedisSource jedisPool() {
		GenericObjectPoolConfig<JedisCommands> cfg = new GenericObjectPoolConfig<JedisCommands>();
		cfg.setMaxTotal(poolParams.getMaxTotal());
		cfg.setMaxIdle(poolParams.getMaxIdle());
		cfg.setMinIdle(poolParams.getMinIdle());
		cfg.setMaxWaitMillis(poolParams.getMaxWaitMillis());
		cfg.setMinEvictableIdleTimeMillis(poolParams.getMinEvictableIdleTimeMillis());
		cfg.setTimeBetweenEvictionRunsMillis(poolParams.getTimeBetweenEvictionRunsMillis());
		cfg.setNumTestsPerEvictionRun(poolParams.getNumTestsPerEvictionRun());
		cfg.setTestOnBorrow(poolParams.getTestOnBorrow());
		cfg.setTestWhileIdle(poolParams.getTestWhileIdle());
		cfg.setTestOnReturn(poolParams.getTestOnReturn());
		cfg.setBlockWhenExhausted(Boolean.parseBoolean(properties.getProperty("spring.redis.pool.blockWhenExhausted")));
		int connectionTimeout = Integer.parseInt(properties.getProperty("spring.redis.timeout"));
		String redisPwd = properties.getProperty("spring.redis.password");
		String _clusterNodesStr = CommonHelper.trim(properties.getProperty("spring.redis.nodes"));
		String[] _clusterNodes = _clusterNodesStr.split(",");
		log.info("REDIS_NODES======================{}", _clusterNodesStr);
		if(_clusterNodes.length>1) {
			Set<String> clusterNodes = new HashSet<String>(Arrays.asList(_clusterNodes));
			String sentinelMaster = CommonHelper.trim(properties.getProperty("spring.redis.sentinel.master"));
			if(sentinelMaster!=null) {//哨兵
				pool = new JedisSentinelPool(sentinelMaster, clusterNodes, cfg, connectionTimeout, redisPwd);
				log.info("REDIS_MODE============SENTINEL");
			} else {//集群
				int soTimeout = Integer.parseInt(properties.getProperty("spring.redis.soTimeout"));
				Set<HostAndPort> clusterNodes_ = new HashSet<HostAndPort>(clusterNodes.size());
				for(String node:clusterNodes) {
					String[] ss = node.split(":");
					clusterNodes_.add(new HostAndPort(ss[0], Integer.valueOf(ss[1])));
				}
				quincyJedis = new QuincyJedis(new JedisCluster(clusterNodes_, connectionTimeout, soTimeout, DEFAULT_MAX_ATTEMPTS, redisPwd, cfg));
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
			pool = new JedisPool(cfg, redisHost, redisPort, connectionTimeout, redisPwd);
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