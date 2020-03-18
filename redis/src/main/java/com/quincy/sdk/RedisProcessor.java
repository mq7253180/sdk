package com.quincy.sdk;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import redis.clients.jedis.Jedis;

public interface RedisProcessor {
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, VCcodeSender sender) throws Exception;
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, HttpServletResponse response, int size, int start, int space, int width, int height) throws Exception;
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, String emailTo, String subject, String content) throws Exception;
	public Result validateVCode(HttpServletRequest request) throws Exception;
	public String createOrGetToken(HttpServletRequest request);
	public Object opt(RedisOperation operation) throws Exception;
	public Object opt(HttpServletRequest request, RedisWebOperation operation) throws Exception;
	public int getMaxFailuresAlloed();
	public int getLoginFailures(HttpServletRequest request, Jedis jedis);
}