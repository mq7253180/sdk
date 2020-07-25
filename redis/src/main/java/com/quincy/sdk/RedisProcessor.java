package com.quincy.sdk;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import redis.clients.jedis.Jedis;

public interface RedisProcessor {
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, String clientTokenName, VCcodeSender sender) throws Exception;
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, String clientTokenName, HttpServletResponse response, int size, int start, int space, int width, int height) throws Exception;
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, String clientTokenName, String emailTo, String subject, String content) throws Exception;
	public Result validateVCode(HttpServletRequest request, String clientTokenName, boolean ignoreCase) throws Exception;
	public String createOrGetToken(HttpServletRequest request, String clientTokenName);
	public Object opt(RedisOperation operation) throws Exception;
	public Object opt(HttpServletRequest request, RedisWebOperation operation, String clientTokenName) throws Exception;
	public int getExpire(HttpServletRequest request);
	public void deleteCookie(HttpServletResponse response);
	public void deleteCookie();
	public void setExpiry(HttpServletRequest request, byte[] key, boolean deleteCookieIfExpired, Jedis jedis);
	public String setAndExpire(String key, String val, int expireSeconds, int retries, long retryIntervalMillis, Jedis jedis);
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds, int retries, long retryIntervalMillis, Jedis jedis);
	public String setAndExpire(String key, String val, int expireSeconds, int retries, long retryIntervalMillis);
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds, int retries, long retryIntervalMillis);
	public String setAndExpire(String key, String val, int expireSeconds, Jedis jedis);
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds, Jedis jedis);
	public String setAndExpire(String key, String val, int expireSeconds);
	public String setAndExpire(byte[] key, byte[] val, int expireSeconds);
}