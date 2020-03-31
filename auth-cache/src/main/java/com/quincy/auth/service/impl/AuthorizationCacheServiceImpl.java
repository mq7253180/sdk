package com.quincy.auth.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quincy.auth.o.DSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthCallback;
import com.quincy.core.InnerConstants;
import com.quincy.core.redis.JedisSource;
import com.quincy.sdk.Client;
import com.quincy.sdk.RedisProcessor;
import com.quincy.sdk.RedisWebOperation;
import com.quincy.sdk.helper.CommonHelper;

import redis.clients.jedis.Jedis;

@Service("authorizationCacheServiceImpl")
public class AuthorizationCacheServiceImpl extends AuthorizationServiceSupport {
	@Autowired
	private JedisSource jedisSource;
	@Autowired
	private RedisProcessor redisProcessor;
	@Resource(name = InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;
	@Resource(name = "sessionKeyPrefix")
	private String sessionKeyPrefix;

	@Override
	protected Object getUserObject(HttpServletRequest request) throws Exception {
		return redisProcessor.opt(request, new RedisWebOperation() {
			@Override
			public Object run(Jedis jedis, String token) throws ClassNotFoundException, IOException {
				byte[] key = (sessionKeyPrefix+token).getBytes();
				byte[] b = jedis.get(key);
				if(b!=null&&b.length>0) {
					jedis.expire(key, getExpireSconds(request));
					return CommonHelper.unSerialize(b);
				} else 
					return null;
			}
		}, null);
	}

	private DSession setSession(HttpServletRequest request, String jsessionid, String originalJsessionid, Long userId, AuthCallback callback) throws IOException, ClassNotFoundException {
		User user = callback.getUser();
		user.setJsessionid(jsessionid);
		user.setPassword(null);
		DSession session = this.createSession(user);
		byte[] key = (sessionKeyPrefix+jsessionid).getBytes();
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			if(originalJsessionid!=null) {//同一user不同客户端登录互踢
				byte[] originalKey = (sessionKeyPrefix+originalJsessionid).getBytes();
				byte[] b = jedis.get(originalKey);
				if(b!=null&&b.length>0) {
					DSession originalSession = (DSession)CommonHelper.unSerialize(b);
					if(originalSession.getUser().getId().equals(userId))
						jedis.del(originalKey);
				}
			}
			int expire = getExpireSconds(request);
			jedis.set(key, CommonHelper.serialize(session));
			jedis.expire(key, expire);
			callback.updateLastLogined(jsessionid);
			return session;
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	private int getExpireSconds(HttpServletRequest request) {
		Client client = CommonHelper.getClient(request);
		Integer expireMinutes = Integer.parseInt(properties.getProperty("expire.session."+client.getName()));
		return expireMinutes*60;
	}

	@Override
	public DSession setSession(HttpServletRequest request, String originalJsessionid, Long userId, AuthCallback callback) throws IOException, ClassNotFoundException {
		String jsessionid = redisProcessor.createOrGetToken(request, null);
		DSession session = this.setSession(request, jsessionid, originalJsessionid, userId, callback);
		return session;
	}

	public void logout(HttpServletRequest request) throws Exception {
		redisProcessor.opt(request, new RedisWebOperation() {
			@Override
			public Object run(Jedis jedis, String token) {
				jedis.del((sessionKeyPrefix+token).getBytes());
				return null;
			}
		}, null);
	}

	private void updateSession(User user, Jedis jedis) throws IOException {
		String jsessionid = CommonHelper.trim(user.getJsessionid());
		if(jsessionid!=null) {
			byte[] key = (sessionKeyPrefix+jsessionid).getBytes();
			byte[] b = jedis.get(key);
			if(b!=null&&b.length>0) {
				DSession session = this.createSession(user);
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
	public <T extends User> void updateSession(List<T> users) throws IOException {
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			for(User user:users) {
				this.updateSession(user, jedis);
			}
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}
}