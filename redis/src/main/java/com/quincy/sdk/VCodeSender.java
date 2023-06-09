package com.quincy.sdk;

public interface VCodeSender {
	public void send(char[] vcode, String token) throws Exception;
}