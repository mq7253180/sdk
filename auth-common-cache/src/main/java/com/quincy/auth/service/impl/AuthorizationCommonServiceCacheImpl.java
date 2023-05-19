package com.quincy.auth.service.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.quincy.sdk.RedisProcessor;
import com.quincy.sdk.RedisWebOperation;
import com.quincy.sdk.helper.CommonHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import redis.clients.jedis.Jedis;

@Service
public class AuthorizationCommonServiceCacheImpl extends AuthorizationCommonServiceSupport {
	@Autowired
	private RedisProcessor redisProcessor;
	@Autowired
	@Qualifier("sessionKeyPrefix")
	private String sessionKeyPrefix;

	@Override
	protected Object getUserObject(HttpServletRequest request) throws Exception {
		Object retVal = redisProcessor.opt(request, new RedisWebOperation() {
			@Override
			public Object run(Jedis jedis, String token) throws ClassNotFoundException, IOException {
				byte[] key = (sessionKeyPrefix+token).getBytes();
				byte[] b = jedis.get(key);
				if(b!=null&&b.length>0) {
					redisProcessor.setExpiry(request, key, true, jedis);
					return CommonHelper.unSerialize(b);
				} else 
					return null;
			}
		}, null);
		if(retVal==null)
			redisProcessor.deleteCookie();
		return retVal;
	}

	@Override
	public void setExpiry(HttpServletRequest request, boolean deleteCookieIfExpired) throws Exception {
		redisProcessor.opt(request, new RedisWebOperation() {
			@Override
			public Object run(Jedis jedis, String token) throws Exception {
				byte[] key = (sessionKeyPrefix+token).getBytes();
				redisProcessor.setExpiry(request, key,deleteCookieIfExpired, jedis);
				return null;
			}
		}, null);
	}

	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
		redisProcessor.opt(request, new RedisWebOperation() {
			@Override
			public Object run(Jedis jedis, String token) {
				jedis.del((sessionKeyPrefix+token).getBytes());
				return null;
			}
		}, null);
		redisProcessor.deleteCookie(response);
	}
}