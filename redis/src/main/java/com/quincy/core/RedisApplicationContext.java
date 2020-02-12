package com.quincy.core;

import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.sdk.Constants;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

@Configuration
public class RedisApplicationContext {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@Bean
    public Pool<Jedis> jedisPool() {
		String redisHost = properties.getProperty("spring.redis.host");
		int redisPort = Integer.parseInt(properties.getProperty("spring.redis.port"));
		String redisPwd = properties.getProperty("spring.redis.password");
		int redisTimeout = Integer.parseInt(properties.getProperty("spring.redis.timeout"));
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
		JedisPool pool = new JedisPool(cfg, redisHost, redisPort, redisTimeout, redisPwd);
		return pool;
	}

	@Bean("cacheKeyPrefix")
	public String cacheKeyPrefix() {
		return properties.getProperty("spring.application.name")+".cache.";
	}
}