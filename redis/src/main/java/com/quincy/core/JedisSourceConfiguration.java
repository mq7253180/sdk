package com.quincy.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PreDestroy;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.redis.JedisSource;
import com.quincy.core.redis.QuincyJedis;
import com.quincy.core.redis.RedisConstants;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.util.Pool;

@Slf4j
@Configuration
public class JedisSourceConfiguration {
	@Value("${spring.application.name}")
	private String applicationName;
	@Value("#{'${spring.redis.nodes}'.split(',')}")
	private String[] _clusterNodes;
	@Value("${spring.data.redis.password}")
	private String redisPwd;
	@Value("${spring.data.redis.timeout}")
	private int connectionTimeout;
	@Value("${spring.data.redis.sentinel.master:#{null}}")
	private String sentinelMaster;
	@Value("${spring.redis.cluster.soTimeout}")
	private int soTimeout;
	@Value("${spring.redis.cluster.maxAttempts}")
	private int maxAttempts;
	@Autowired
	private GenericObjectPoolConfig poolCfg;

	private static Pool<Jedis> pool;
	private static QuincyJedis quincyJedis;

	@Bean(RedisConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
    public JedisSource jedisPool() {
		if(_clusterNodes.length>1) {
			Set<String> clusterNodes = new HashSet<String>(Arrays.asList(_clusterNodes));
			if(sentinelMaster!=null) {//哨兵
				pool = new JedisSentinelPool(sentinelMaster, clusterNodes, poolCfg, connectionTimeout, redisPwd);
				log.info("REDIS_MODE============SENTINEL");
			} else {//集群
				Set<HostAndPort> clusterNodes_ = new HashSet<HostAndPort>(clusterNodes.size());
				for(String node:clusterNodes) {
					String[] ss = node.split(":");
					clusterNodes_.add(new HostAndPort(ss[0], Integer.valueOf(ss[1])));
				}
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
		return "CACHE:"+applicationName+":";
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