package com.quincy.auth.o;

public interface User {
	public Long getId();
	public void setPassword(String password);
	public String getPassword();
	public void setJsessionid(String jsessionid);
	public String getJsessionid();
}