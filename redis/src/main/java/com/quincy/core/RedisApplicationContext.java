package com.quincy.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PreDestroy;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.redis.JedisSource;
import com.quincy.core.redis.QuincyJedis;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.util.Pool;

@Slf4j
@Configuration
public class RedisApplicationContext {
	@Value("${spring.application.name}")
	private String applicationName;
	@Value("#{'${spring.redis.nodes}'.split(',')}")
	private String[] _clusterNodes;
	@Value("${spring.redis.password}")
	private String redisPwd;
	@Value("${spring.redis.timeout}")
	private int connectionTimeout;
	@Autowired
	@Qualifier(InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;
	@Autowired
	private GenericObjectPoolConfig poolCfg;

	private static Pool<Jedis> pool;
	private static QuincyJedis quincyJedis;

	@Bean(InnerConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
    public JedisSource jedisPool() {
		if(_clusterNodes.length>1) {
			Set<String> clusterNodes = new HashSet<String>(Arrays.asList(_clusterNodes));
			String sentinelMaster = CommonHelper.trim(properties.getProperty("spring.redis.sentinel.master"));
			if(sentinelMaster!=null) {//哨兵
				pool = new JedisSentinelPool(sentinelMaster, clusterNodes, poolCfg, connectionTimeout, redisPwd);
				log.info("REDIS_MODE============SENTINEL");
			} else {//集群
				Set<HostAndPort> clusterNodes_ = new HashSet<HostAndPort>(clusterNodes.size());
				for(String node:clusterNodes) {
					String[] ss = node.split(":");
					clusterNodes_.add(new HostAndPort(ss[0], Integer.valueOf(ss[1])));
				}
				int soTimeout = Integer.parseInt(properties.getProperty("spring.redis.cluster.soTimeout"));
				int maxAttempts = Integer.parseInt(properties.getProperty("spring.redis.cluster.maxAttempts"));
				quincyJedis = new QuincyJedis(new JedisCluster(clusterNodes_, connectionTimeout, soTimeout, maxAttempts, redisPwd, poolCfg));
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
			pool = new JedisPool(poolCfg, redisHost, redisPort, connectionTimeout, redisPwd);
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
		return applicationName+".cache.";
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