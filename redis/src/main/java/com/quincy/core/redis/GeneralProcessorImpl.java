package com.quincy.core.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.quincy.sdk.RedisProcessor;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

@Slf4j
@Component
public class GeneralProcessorImpl implements RedisProcessor {
	@Autowired
	@Qualifier(RedisConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
	private JedisSource jedisSource;
	@Value("${spring.application.name}")
	private String applicationName;

	/*
	private void deleteCookie(HttpServletResponse response, String clientTokenName) {
		this.addCookie(response, clientTokenName, "", 0);
	}

	@Value("${domain}")
	private String domain;

	private void addCookie(HttpServletResponse response, String key, String value, int expiry) {
		Cookie cookie = new Cookie(key, value);
		cookie.setDomain(domain);
		cookie.setPath("/");
		cookie.setMaxAge(expiry);
		response.addCookie(cookie);
	}
	*/

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
			if((TYPE_FLAG_STRING.equalsIgnoreCase(typeFlag)&&jedis.exists(keyInString))||(TYPE_FLAG_BYTES.equalsIgnoreCase(typeFlag)&&jedis.exists(keyInBytes))) {
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