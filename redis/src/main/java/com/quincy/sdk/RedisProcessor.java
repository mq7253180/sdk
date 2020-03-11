package com.quincy.sdk;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RedisProcessor {
	public Object opt(RedisOperation operation) throws Exception;
	public Object opt(HttpServletRequest request, RedisWebOperation operation) throws Exception;
	public void vcode(HttpServletRequest request, HttpServletResponse response, int length) throws IOException;
//	public void cacheStr(HttpServletRequest request, String flag, String content) throws Exception;
//	public String getCachedStr(HttpServletRequest request, String flag) throws Exception;
	public String createOrGetToken(HttpServletRequest request);
}