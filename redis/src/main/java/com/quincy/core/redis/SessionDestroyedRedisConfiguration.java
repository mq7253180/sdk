package com.quincy.core.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.data.redis.RedisSessionRepository;

import com.quincy.core.SessionInvalidation;

public class SessionDestroyedRedisConfiguration implements SessionInvalidation {
	@Autowired
	private RedisSessionRepository redisSessionRepository;

	@Override
	public void invalidate(String jsessionid) {
		redisSessionRepository.deleteById(jsessionid);
	}
}