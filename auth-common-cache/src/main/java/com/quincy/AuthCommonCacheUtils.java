package com.quincy;

import java.util.Properties;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.quincy.core.InnerConstants;
import com.quincy.sdk.Client;
import com.quincy.sdk.helper.CommonHelper;

import redis.clients.jedis.Jedis;

@Component
public class AuthCommonCacheUtils {
	@Resource(name = InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	public void setExpiry(HttpServletRequest request, Jedis jedis, byte[] key) {
		Client client = CommonHelper.getClient(request);
		Integer expireMinutes = Integer.parseInt(properties.getProperty("expire.session."+client.getName()));
		int expire = expireMinutes*60;
		jedis.expire(key, expire);
	}
}