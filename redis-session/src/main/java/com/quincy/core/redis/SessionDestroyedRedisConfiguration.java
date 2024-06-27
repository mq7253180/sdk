package com.quincy.core.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.data.redis.RedisSessionRepository;

import com.quincy.core.BaseSessionDestroyedConfiguration;
import com.quincy.core.SessionInvalidation;
import com.quincy.sdk.annotation.EnableRedisSessionEviction;

public class SessionDestroyedRedisConfiguration extends BaseSessionDestroyedConfiguration implements SessionInvalidation {
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