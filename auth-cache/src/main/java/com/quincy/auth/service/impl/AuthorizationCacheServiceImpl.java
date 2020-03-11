package com.quincy.auth.service.impl;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.quincy.auth.o.DSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthCallback;
import com.quincy.core.redis.JedisSource;
import com.quincy.sdk.RedisProcessor;
import com.quincy.sdk.RedisWebOperation;
import com.quincy.sdk.helper.CommonHelper;

import redis.clients.jedis.Jedis;

@Service("authorizationCacheServiceImpl")
public class AuthorizationCacheServiceImpl extends AuthorizationAbstract {
	@Autowired
	private JedisSource jedisSource;
	@Autowired
	private RedisProcessor redisProcessor;
	@Value("${expire.session}")
	private int sessionExpire;
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
					int seconds = sessionExpire*60;
					jedis.expire(key, seconds);
					return CommonHelper.unSerialize(b);
				} else 
					return null;
			}
		});
	}

	private DSession setSession(String jsessionid, String originalJsessionid, Long userId, AuthCallback callback) throws IOException, ClassNotFoundException {
		User user = callback.getUser();
		user.setJsessionid(jsessionid);
		DSession session = this.createSession(user);
		byte[] key = (sessionKeyPrefix+jsessionid).getBytes();
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			if(originalJsessionid!=null&&originalJsessionid.length()>0) {//同一user不同客户端登录互踢
				byte[] originalKey = (sessionKeyPrefix+originalJsessionid).getBytes();
				byte[] b = jedis.get(originalKey);
				if(b!=null&&b.length>0) {
					DSession originalSession = (DSession)CommonHelper.unSerialize(b);
					if(originalSession.getUser().getId().equals(userId))
						jedis.del(originalKey);
				}
			}
			jedis.set(key, CommonHelper.serialize(session));
			int seconds = sessionExpire*60;
			jedis.expire(key, seconds);
			callback.updateLastLogined(jsessionid);
			return session;
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	@Override
	public DSession setSession(HttpServletRequest request, String originalJsessionid, Long userId, AuthCallback callback) throws IOException, ClassNotFoundException {
		String jsessionid = redisProcessor.createOrGetToken(request);
		DSession session = this.setSession(jsessionid, originalJsessionid, userId, callback);
		return session;
	}

	public void logout(HttpServletRequest request) throws Exception {
		redisProcessor.opt(request, new RedisWebOperation() {
			@Override
			public Object run(Jedis jedis, String token) throws Exception {
				jedis.del((sessionKeyPrefix+token).getBytes());
				return null;
			}
		});
	}

	private void updateSession(User user, Jedis jedis) throws IOException {
		byte[] key = (sessionKeyPrefix+user.getJsessionid()).getBytes();
		byte[] b = jedis.get(key);
		if(b!=null&&b.length>0) {
			DSession session = this.createSession(user);
			b = CommonHelper.serialize(session);
			jedis.set(key, b);
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