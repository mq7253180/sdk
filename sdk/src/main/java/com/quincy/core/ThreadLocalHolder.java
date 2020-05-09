package com.quincy.core;

import org.springframework.core.NamedThreadLocal;

public class ThreadLocalHolder {
	private static ThreadLocal<String> accsessToken = null;

	public static void setAccsessToken(String token) {
		accsessToken = new NamedThreadLocal<String>("accsessToken");
		accsessToken.set(token);
	}

	public static String getAccsessToken() {
		return accsessToken==null?null:accsessToken.get();
	}
}