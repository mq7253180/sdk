package com.quincy.auth;

public interface SessionInvalidation {
	public void invalidate(String jsessionid);
	public boolean pcBrowserEvict();
	public boolean mobileBrowserEvict();
	public boolean appEvict();
}