package com.quincy.core;

import java.util.Properties;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.quincy.sdk.Client;
import com.quincy.sdk.annotation.JedisInjector;
import com.quincy.sdk.helper.CommonHelper;

import redis.clients.jedis.Jedis;

@Component
public class AuthCacheUtils {
	@Resource(name = InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@JedisInjector
	public void setExpiry(HttpServletRequest request, Jedis jedis, byte[] key) {
		int expire = this.getExpire(request);
		jedis.expire(key, expire);
	}

	public int getExpire(HttpServletRequest request) {
		Client client = CommonHelper.getClient(request);
		Integer expireMinutes = Integer.parseInt(properties.getProperty("expire.session."+client.getName()));
		int expire = expireMinutes*60;
		return expire;
	}
}