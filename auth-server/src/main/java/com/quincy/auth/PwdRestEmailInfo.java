package com.quincy.auth;

public interface PwdRestEmailInfo {
	public String getSubject();
	public String getContent(String uri);
}