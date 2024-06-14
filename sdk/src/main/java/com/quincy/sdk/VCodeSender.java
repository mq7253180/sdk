package com.quincy.sdk;

public interface VCodeSender {
	public void send(char[] vcode, int expireMinuts) throws Exception;
}