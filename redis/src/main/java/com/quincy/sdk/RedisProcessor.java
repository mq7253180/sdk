package com.quincy.sdk;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RedisProcessor {
	public Object opt(RedisOperation operation) throws Exception;
	public Object opt(HttpServletRequest request, RedisWebOperation operation) throws Exception;
	public void vcode(HttpServletRequest request, HttpServletResponse response, int size, int start, int space, int width, int height) throws IOException;
	public String getCachedVCode(HttpServletRequest request) throws Exception;
	public String createOrGetToken(HttpServletRequest request);
}