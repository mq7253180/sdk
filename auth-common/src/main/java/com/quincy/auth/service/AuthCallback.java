package com.quincy.auth.service;

import com.quincy.auth.o.User;

public interface AuthCallback {
	public void updateLastLogined(String jsessionid);
	public User getUser();
}
