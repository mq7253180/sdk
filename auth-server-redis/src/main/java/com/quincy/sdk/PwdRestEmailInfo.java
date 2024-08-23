package com.quincy.sdk;

public interface PwdRestEmailInfo {
	public String getSubject();
	public String getContent(String uri, int timeoutSeconds);
}