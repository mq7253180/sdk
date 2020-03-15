package com.quincy.sdk;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RedisProcessor {
	public char[] vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, VCcodeSender sender) throws Exception;
	public char[] vcode(HttpServletRequest request, HttpServletResponse response, VCodeCharsFrom charsFrom, int size, int start, int space, int width, int height) throws Exception;
	public char[] vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, String emailTo, String subject, String content) throws Exception;
	public String createOrGetToken(HttpServletRequest request);
	public Object opt(RedisOperation operation) throws Exception;
	public Object opt(HttpServletRequest request, RedisWebOperation operation) throws Exception;
}