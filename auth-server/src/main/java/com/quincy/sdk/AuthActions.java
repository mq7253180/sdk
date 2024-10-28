package com.quincy.sdk;

import java.util.Map;

public interface AuthActions {
	public abstract void loadAttributes(Long userId, Map<String, Object> attributes);
}