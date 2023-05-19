package com.quincy.auth.service.impl;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.quincy.auth.o.XSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthCallback;
import com.quincy.core.InnerConstants;
import com.quincy.core.redis.JedisSource;
import com.quincy.sdk.RedisProcessor;
import com.quincy.sdk.helper.CommonHelper;

import jakarta.servlet.http.HttpServletRequest;
import redis.clients.jedis.Jedis;

@Service
public class AuthorizationServerServiceCacheImpl extends AuthorizationServerServiceSupport {
	@Autowired
	@Qualifier(InnerConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
	private JedisSource jedisSource;
	@Autowired
	private RedisProcessor redisProcessor;
	@Autowired
	@Qualifier("sessionKeyPrefix")
	private String sessionKeyPrefix;

	private XSession setSession(HttpServletRequest request, String jsessionid, String originalJsessionid, Long userId, AuthCallback callback) throws IOException, ClassNotFoundException {
		User user = callback.getUser();
		user.setJsessionid(jsessionid);
		user.setPassword(null);
		XSession session = this.createSession(user);
		byte[] key = (sessionKeyPrefix+jsessionid).getBytes();
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			if(originalJsessionid!=null) {//同一user不同客户端登录互踢
				byte[] originalKey = (sessionKeyPrefix+originalJsessionid).getBytes();
				byte[] b = jedis.get(originalKey);
				if(b!=null&&b.length>0) {
					XSession originalSession = (XSession)CommonHelper.unSerialize(b);
					if(originalSession.getUser().getId().equals(userId))
						jedis.del(originalKey);
				}
			}
			redisProcessor.setAndExpire(key, CommonHelper.serialize(session), redisProcessor.getExpire(request), jedis);
			callback.updateLastLogined(jsessionid);
			return session;
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	@Override
	public XSession setSession(HttpServletRequest request, String originalJsessionid, Long userId, AuthCallback callback) throws IOException, ClassNotFoundException {
		String jsessionid = redisProcessor.createOrGetToken(request, null);
		XSession session = this.setSession(request, jsessionid, originalJsessionid, userId, callback);
		return session;
	}

	private void updateSession(User user, Jedis jedis) throws IOException {
		String jsessionid = CommonHelper.trim(user.getJsessionid());
		if(jsessionid!=null) {
			byte[] key = (sessionKeyPrefix+jsessionid).getBytes();
			byte[] b = jedis.get(key);
			if(b!=null&&b.length>0) {
				XSession session = this.createSession(user);
				b = CommonHelper.serialize(session);
				jedis.set(key, b);
			}
		}
	}

	@Override
	public void updateSession(User user) throws IOException {
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			this.updateSession(user, jedis);
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	@Override
	public void updateSession(List<User> users) throws IOException {
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			for(User user:users)
				this.updateSession(user, jedis);
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}
}