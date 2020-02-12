package com.quincy.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.sdk.Constants;
import com.quincy.sdk.helper.CommonHelper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.util.Pool;

@Configuration
public class RedisApplicationContext {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@Bean
    public Pool<Jedis> jedisPool() {
//		int redisMaxActive = Integer.parseInt(properties.getProperty("spring.redis.pool.max-active"));
		long redisMaxWait = Long.parseLong(properties.getProperty("spring.redis.pool.max-wait"));
		int redisMaxTotal = Integer.parseInt(properties.getProperty("spring.redis.pool.max-total"));
		int redisMaxIdle = Integer.parseInt(properties.getProperty("spring.redis.pool.max-idle"));
		int redisMinIdle = Integer.parseInt(properties.getProperty("spring.redis.pool.min-idle"));
		GenericObjectPoolConfig<Jedis> cfg = new GenericObjectPoolConfig<Jedis>();
		cfg.setMaxTotal(redisMaxTotal);
		cfg.setMaxIdle(redisMaxIdle);
		cfg.setMinIdle(redisMinIdle);
		cfg.setMaxWaitMillis(redisMaxWait);
		int redisTimeout = Integer.parseInt(properties.getProperty("spring.redis.timeout"));
		String redisPwd = properties.getProperty("spring.redis.password");
		String sentinelMaster = CommonHelper.trim(properties.getProperty("spring.redis.sentinel.master"));
		String sentinelNodes = CommonHelper.trim(properties.getProperty("spring.redis.sentinel.nodes"));
		Pool<Jedis> pool = null;
		if(sentinelNodes!=null&&sentinelMaster!=null) {
			Set<String> sentinels = new HashSet<String>(Arrays.asList(sentinelNodes.split(",")));
			pool = new JedisSentinelPool(sentinelMaster, sentinels, cfg, redisTimeout, redisPwd);
		} else {
			String redisHost = properties.getProperty("spring.redis.host");
			int redisPort = Integer.parseInt(properties.getProperty("spring.redis.port"));
			pool = new JedisPool(cfg, redisHost, redisPort, redisTimeout, redisPwd);
		}
		return pool;
	}

	@Bean("cacheKeyPrefix")
	public String cacheKeyPrefix() {
		return properties.getProperty("spring.application.name")+".cache.";
	}
}