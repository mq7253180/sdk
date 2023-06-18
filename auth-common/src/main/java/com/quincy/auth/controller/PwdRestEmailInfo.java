package com.quincy.auth.controller;

public interface PwdRestEmailInfo {
	public String getSubject();
	public String getContent(String uri);
}