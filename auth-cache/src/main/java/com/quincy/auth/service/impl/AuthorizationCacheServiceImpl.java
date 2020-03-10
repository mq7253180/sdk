package com.quincy.auth.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.quincy.auth.o.DSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthCallback;
import com.quincy.core.InnerConstants;
import com.quincy.core.redis.JedisSource;
import com.quincy.sdk.helper.CommonHelper;

import redis.clients.jedis.Jedis;

@Service("authorizationCacheServiceImpl")
public class AuthorizationCacheServiceImpl extends AuthorizationAbstract {
	@Autowired
	private JedisSource jedisSource;
	@Value("${expire.session}")
	private int sessionExpire;
	@Value("${domain}")
	private String domain;
	private final static String FLAG_VCODE = "vcode";
	@Resource(name = "sessionKeyPrefix")
	private String sessionKeyPrefix;
	@Value("${spring.application.name}")
	private String appName;

	@Override
	protected Object getUserObject(HttpServletRequest request) throws Exception {
		return new Decoration() {
			@Override
			protected Object run(Jedis jedis, String token) throws Exception {
				byte[] key = (sessionKeyPrefix+token).getBytes();
				byte[] b = jedis.get(key);
				if(b!=null&&b.length>0) {
					int seconds = sessionExpire*60;
					jedis.expire(key, seconds);
					return CommonHelper.unSerialize(b);
				} else 
					return null;
			}
		}.start(request);
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
		String jsessionid = this.createOrGetToken(request);
		DSession session = this.setSession(jsessionid, originalJsessionid, userId, callback);
		return session;
	}

	public void logout(HttpServletRequest request) throws Exception {
		new Decoration() {
			@Override
			protected Object run(Jedis jedis, String token) throws Exception {
				jedis.del((sessionKeyPrefix+token).getBytes());
				return null;
			}
		}.start(request);
	}

	protected void saveVcode(HttpServletRequest request, String vcode) throws Exception {
		this.setCachedStr(request, FLAG_VCODE, vcode, 2);
	}

	public String getCachedVcode(HttpServletRequest request) throws Exception {
		return this.getCachedStr(request, FLAG_VCODE);
	}

	private abstract class Decoration {
		protected abstract Object run(Jedis jedis, String token) throws Exception;

		public Object start(HttpServletRequest request) throws Exception {
			String token = CommonHelper.trim(CommonHelper.getValue(request, InnerConstants.CLIENT_TOKEN));
			if(token!=null) {
				Jedis jedis = null;
				try {
					jedis = jedisSource.get();
					return this.run(jedis, token);
				} finally {
					if(jedis!=null)
						jedis.close();
				}
			}
			return null;
		}
	}

	private String createOrGetToken(HttpServletRequest request) {
		String token = CommonHelper.trim(CommonHelper.getValue(request, InnerConstants.CLIENT_TOKEN));
		if(token==null) {
			token = UUID.randomUUID().toString().replaceAll("-", "");
			Cookie cookie = new Cookie(InnerConstants.CLIENT_TOKEN, token);
			cookie.setDomain(domain);
			cookie.setPath("/");
			cookie.setMaxAge(3600*12);
			HttpServletResponse response = CommonHelper.getResponse();
			response.addCookie(cookie);
		}
		return token;
	}

	private void setCachedStr(HttpServletRequest request, String flag, String content, int expire) {
		String token = this.createOrGetToken(request);
		String key = appName+"."+flag+"."+token;
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			jedis.set(key, content);
			int seconds = expire*60;
			jedis.expire(key, seconds);
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	public String getCachedStr(HttpServletRequest request, String flag) throws Exception {
		Object retVal = new Decoration() {
			@Override
			protected Object run(Jedis jedis, String token) throws Exception {
				String key = appName+"."+flag+"."+token;
				String vcode = jedis.get(key);
				return vcode;
			}
		}.start(request);
		return retVal==null?null:String.valueOf(retVal);
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