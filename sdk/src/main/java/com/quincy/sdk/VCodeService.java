package com.quincy.sdk;

import com.quincy.core.VCodeStore;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public interface VCodeService {
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, VCodeSender sender) throws Exception;
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, String emailTo, String subject, String _content) throws Exception;
	public void outputVcode(VCodeStore vCodeStore, HttpServletResponse response, int size, int start, int space, int width, int height) throws Exception;
	public void saveVcode(HttpSession session, char[] vcode, String attrKey);
}