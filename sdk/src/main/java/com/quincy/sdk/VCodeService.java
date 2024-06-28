package com.quincy.sdk;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface VCodeService {
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, VCodeSender sender) throws Exception;
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, String emailTo, String subject, String _content) throws Exception;
	public Result validateVCode(HttpServletRequest request, boolean ignoreCase, String attrKey) throws Exception;
	public void outputVcode(HttpServletRequest request, HttpServletResponse response, int size, int start, int space, int width, int height) throws Exception;
}