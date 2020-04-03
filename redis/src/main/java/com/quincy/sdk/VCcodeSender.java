package com.quincy.sdk;

public interface VCcodeSender {
	public void send(char[] vcode, String token) throws Exception;
}