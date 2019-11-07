package com.quincy.auth.o;

public interface User {
	public Long getId();
	public String getPassword();
	public void setJsessionid(String jsessionid);
	public String getJsessionid();
}
