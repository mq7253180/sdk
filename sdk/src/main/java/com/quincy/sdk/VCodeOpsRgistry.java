package com.quincy.sdk;

import jakarta.servlet.http.HttpServletRequest;

public interface VCodeOpsRgistry {
	public char[] generate(VCodeCharsFrom _charsFrom, int length);
	public Result validate(HttpServletRequest request, boolean ignoreCase, String attrKey) throws Exception;
	public Result validate(HttpServletRequest request, boolean ignoreCase, String attrKey, String msgKey) throws Exception;
	public String genAndSend(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, VCodeSender sender) throws Exception;
	public String genAndSend(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, String emailTo, String subject, String _content) throws Exception;
}