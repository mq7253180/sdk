package com.quincy.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.data.redis.RedisSessionRepository;

import com.quincy.auth.BaseSessionDestroyedConfiguration;
import com.quincy.sdk.annotation.auth.EnableRedisSessionEviction;

public class SessionDestroyedRedisConfiguration extends BaseSessionDestroyedConfiguration {
	@Autowired
	private RedisSessionRepository redisSessionRepository;

	@Override
	public void invalidate(String jsessionid) {
		redisSessionRepository.deleteById(jsessionid);
	}

	@Override
	protected Class<?> annotationClass() {
		return EnableRedisSessionEviction.class;
	}
}