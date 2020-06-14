package com.quincy;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.quincy.sdk.Client;
import com.quincy.sdk.helper.CommonHelper;

import redis.clients.jedis.Jedis;

public class AuthUtils {
	public static void setExpiry(HttpServletRequest request, Jedis jedis, byte[] key, Properties properties) {
		Client client = CommonHelper.getClient(request);
		Integer expireMinutes = Integer.parseInt(properties.getProperty("expire.session."+client.getName()));
		int expire = expireMinutes*60;
		jedis.expire(key, expire);
	}
}