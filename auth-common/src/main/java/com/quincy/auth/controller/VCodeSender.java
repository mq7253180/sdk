package com.quincy.auth.controller;

public interface VCodeSender {
	public void send(char[] vcode) throws Exception;
}