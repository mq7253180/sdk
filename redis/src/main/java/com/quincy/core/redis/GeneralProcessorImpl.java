package com.quincy.core.redis;

import java.util.Properties;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.annotation.VCodeRequired;
import com.quincy.core.InnerConstants;
import com.quincy.core.InnerHelper;
import com.quincy.sdk.Client;
import com.quincy.sdk.RedisOperation;
import com.quincy.sdk.RedisProcessor;
import com.quincy.sdk.RedisWebOperation;
import com.quincy.sdk.Result;
import com.quincy.sdk.annotation.JedisSupport;
import com.quincy.sdk.helper.CommonHelper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

@Slf4j
@Component
public class GeneralProcessorImpl extends HandlerInterceptorAdapter implements RedisProcessor {
	@Autowired
	@Qualifier(InnerConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
	private JedisSource jedisSource;
	@Autowired
	@Qualifier(InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;
	@Value("${spring.application.name}")
	private String applicationName;
	private final static String FLAG_VCODE = "vcode";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			VCodeRequired annotation = method.getMethod().getDeclaredAnnotation(VCodeRequired.class);
			if(annotation!=null) {
				Result result = this.validateVCode(request, annotation.clientTokenName(), annotation.ignoreCase());
				if(result.getStatus()!=1) {
					InnerHelper.outputOrForward(request, response, handler, result.getStatus(), result.getMsg(), annotation.timeoutForwardTo(), false);
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public Result validateVCode(HttpServletRequest request, String _clientTokenName, boolean ignoreCase) throws Exception {
		String clientTokenName = CommonHelper.trim(_clientTokenName);
		if(clientTokenName!=null)//校验是否为空
			this.createOrGetToken(request, clientTokenName);
		String inputedVCode = CommonHelper.trim(request.getParameter(InnerConstants.ATTR_VCODE));
		Integer status = null;
		String msgI18NKey = null;
		String msg = null;
		if(inputedVCode==null) {
			status = -5;
			msgI18NKey = "vcode.null";
		} else {
			String cachedVCode = CommonHelper.trim(this.getCachedVCode(request, clientTokenName));
			if(cachedVCode==null) {
				status = -6;
				msgI18NKey = "vcode.expire";
			} else if(!(ignoreCase?cachedVCode.equalsIgnoreCase(inputedVCode):cachedVCode.equals(inputedVCode))) {
				status = -7;
				msgI18NKey = "vcode.not_matched";
			}
		}
		if(status==null) {
			this.rmCachedVCode(request, clientTokenName);
			status = 1;
		} else {
			RequestContext requestContext = new RequestContext(request);
			msg = requestContext.getMessage(msgI18NKey);
		}
		return new Result(status, msg);
	}

	@Override
	public Object opt(RedisOperation operation) throws Exception {
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			return operation.run(jedis);
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	@Override
	public Object opt(HttpServletRequest request, RedisWebOperation operation, String _clientTokenName) throws Exception {
		String clientTokenName = CommonHelper.trim(_clientTokenName);
		if(clientTokenName==null)
			clientTokenName = CommonHelper.trim(properties.getProperty(InnerConstants.CLIENT_TOKEN_PROPERTY_NAME));
		String token = CommonHelper.getValue(request, clientTokenName);
		return token==null?null:this.opt(new RedisOperation() {
			@Override
			public Object run(Jedis jedis) throws Exception {
				return operation.run(jedis, token);
			}
		});
	}

	/*
	private String cacheStr(HttpServletRequest request, String flag, String content, String clientTokenName, int expireSeconds) {
		String token = this.createOrGetToken(request, clientTokenName);
		String key = combineAsKey(flag, token);
		String status = this.setAndExpire(key, content, expireSeconds);
		if("OK".equalsIgnoreCase(status)) {
			return token;
		} else
			throw new RuntimeException("Redis set and expire failed.");
	}
	*/

	private String getCachedStr(HttpServletRequest request, String flag, String clientTokenName) throws Exception {
		Object retVal = this.opt(request, new RedisWebOperation() {
			@Override
			public Object run(Jedis jedis, String token) {
				String key = combineAsKey(flag, token);
				String vcode = jedis.get(key);
				return vcode;
			}
		}, clientTokenName);
		return retVal==null?null:String.valueOf(retVal);
	}

	private void rmCachedStr(HttpServletRequest request, String flag, String clientTokenName) {
		String key = combineAsKey(flag, this.createOrGetToken(request, clientTokenName));
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			jedis.del(key);
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	private String combineAsKey(String flag, String token) {
		return applicationName+"."+flag+"."+token;
	}

	@Override
	public String createOrGetToken(HttpServletRequest request, String _clientTokenName) {
		boolean autoGenerateIfNull = false;
		String clientTokenName = CommonHelper.trim(_clientTokenName);
		if(clientTokenName==null) {
			autoGenerateIfNull = true;
			clientTokenName = CommonHelper.trim(properties.getProperty(InnerConstants.CLIENT_TOKEN_PROPERTY_NAME));
		}
		String token = CommonHelper.getValue(request, clientTokenName);
		if(token==null) {
			if(autoGenerateIfNull) {
				token = System.currentTimeMillis()+"-"+UUID.randomUUID().toString().replaceAll("-", "");
				this.addCookie(CommonHelper.getResponse(), clientTokenName, token, this.getExpire(request)*2);
			} else
				throw new RuntimeException("No value of "+clientTokenName+" is presented.");
		}
		return token;
	}

	@Override
	public void deleteCookie() {
		this.deleteCookie(CommonHelper.getResponse());
	}

	@Override
	public void deleteCookie(HttpServletResponse response) {
		String clientTokenName = CommonHelper.trim(properties.getProperty(InnerConstants.CLIENT_TOKEN_PROPERTY_NAME));
		if(clientTokenName!=null)
			this.deleteCookie(response, clientTokenName);
	}

	private void deleteCookie(HttpServletResponse response, String clientTokenName) {
		this.addCookie(response, clientTokenName, "", 0);
	}

	@JedisSupport
	@Override
	public void setExpiry(HttpServletRequest request, byte[] key, boolean deleteCookieIfExpired, Jedis jedis) {
		int expire = this.getExpire(request);
		long result = jedis.expire(key, expire);
		String clientTokenName = CommonHelper.trim(properties.getProperty(InnerConstants.CLIENT_TOKEN_PROPERTY_NAME));
		if(clientTokenName!=null) {
			HttpServletResponse response = CommonHelper.getResponse();
			if(deleteCookieIfExpired&&result<1) {
				this.deleteCookie(response, clientTokenName);
			} else {
				String token = CommonHelper.getValue(request, clientTokenName);
				if(token!=null)
					this.addCookie(response, clientTokenName, token, expire*2);
			}
		}
	}

	@Override
	public int getExpire(HttpServletRequest request) {
		Client client = CommonHelper.getClient(request);
		Integer expireMinutes = Integer.parseInt(properties.getProperty("expire.session."+client.getName()));
		int expire = expireMinutes*60;
		return expire;
	}

	private void addCookie(HttpServletResponse response, String key, String value, int expiry) {
		Cookie cookie = new Cookie(key, value);
		cookie.setDomain(CommonHelper.trim(properties.getProperty("domain")));
		cookie.setPath("/");
		cookie.setMaxAge(expiry);
		response.addCookie(cookie);
	}

	private String getCachedVCode(HttpServletRequest request, String clientTokenName) throws Exception {
		return this.getCachedStr(request, FLAG_VCODE, clientTokenName);
	}

	private void rmCachedVCode(HttpServletRequest request, String clientTokenName) {
		this.rmCachedStr(request, FLAG_VCODE, clientTokenName);
	}

	private final static String TYPE_FLAG_STRING = "S";
	private final static String TYPE_FLAG_BYTES = "B";

	private String setAndExpire(String typeFlag, String keyInString, String valInString, byte[] keyInBytes, byte[] valInBytes, int expireSeconds, int retries, long retryIntervalMillis, Jedis jedis) {
		/*
		 * NX -- Only set the key if it does not already exist. XX -- Only set the key if it already exist.
		 * EX = seconds; PX = milliseconds
		 */
		String status = null;
		do {
			SetParams params = new SetParams();
			params.ex(expireSeconds);
			if(jedis.exists(keyInString)) {
				params.xx();
			} else 
				params.nx();
			if(TYPE_FLAG_STRING.equalsIgnoreCase(typeFlag)) {
//				status = jedis.set(keyInString, valInString, jedis.exists(keyInString)?"XX":"NX", "EX", expireSeconds);
				status = jedis.set(keyInString, valInString, params);
			} else if(TYPE_FLAG_BYTES.equalsIgnoreCase(typeFlag)) {
//				status = jedis.set(keyInBytes, valInBytes, (jedis.exists(keyInBytes)?"XX":"NX").getBytes(), "EX".getBytes(), expireSeconds);
				status = jedis.set(keyInBytes, valInBytes, params);
			} else
				throw new RuntimeException("Enum value for type flag is illegal. Only 'S' or 'B' are acceptable.");
			if("OK".equalsIgnoreCase(status)) {
				break;
			} else
				try {
					Thread.sleep(retryIntervalMillis);
				} catch (InterruptedException e) {
					log.error("REDIS_SET_AND_EXPIRE_ERR===================", e);
				}
		} while(retries-->0);
		return status;
	}

	@Override
	public String setAndExpire(String key, String val, int expireSeconds, int retries, long retryIntervalMillis, Jedis jedis) {
		return this.setAndExpire(TYPE_FLAG_STRING, key, val, null, null, expireSeconds, retries, retryIntervalMillis, jedis);
	}

	@Override
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds, int retries, long retryIntervalMillis, Jedis jedis) {
		return this.setAndExpire(TYPE_FLAG_BYTES, null, null, key, val, expireSeconds, retries, retryIntervalMillis, jedis);
	}

	@Override
	public String setAndExpire(String key, String val, int expireSeconds, int retries, long retryIntervalMillis) {
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			return this.setAndExpire(key, val, expireSeconds, retries, retryIntervalMillis, jedis);
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	@Override
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds, int retries, long retryIntervalMillis) {
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			return this.setAndExpire(key, val, expireSeconds, retries, retryIntervalMillis, jedis);
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	private final static int DEFAULT_RETRIES = 5;
	private final static long DEFAULT_RETRIE_INTERVAL_MILLIS = 100;

	@Override
	public String setAndExpire(String key, String val, int expireSeconds, Jedis jedis) {
		return this.setAndExpire(key, val, expireSeconds, DEFAULT_RETRIES, DEFAULT_RETRIE_INTERVAL_MILLIS, jedis);
	}

	@Override
	public String setAndExpire(String key, String val, int expireSeconds) {
		return this.setAndExpire(key, val, expireSeconds, DEFAULT_RETRIES, DEFAULT_RETRIE_INTERVAL_MILLIS);
	}

	@Override
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds, Jedis jedis) {
		return this.setAndExpire(key, val, expireSeconds, DEFAULT_RETRIES, DEFAULT_RETRIE_INTERVAL_MILLIS, jedis);
	}

	@Override
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds) {
		return this.setAndExpire(key, val, expireSeconds, DEFAULT_RETRIES, DEFAULT_RETRIE_INTERVAL_MILLIS);
	}
}