package com.quincy.sdk;

public interface AuthActions {
	public abstract void onLogin(Long userId);
	public abstract void sms(String mobilePhone, String vcode, int expireMinuts);
}