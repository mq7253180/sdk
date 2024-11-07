package com.quincy.sdk;

import java.io.Serializable;
import java.util.Map;

public interface AuthActions {
	public abstract void onLogin(Long userId, Map<String, Serializable> attributes);
	public abstract void sms(String mobilePhone, String vcode, int expireMinuts);
}