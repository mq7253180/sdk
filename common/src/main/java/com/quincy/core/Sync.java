package com.quincy.core;

import java.util.Locale;

import org.springframework.core.NamedThreadLocal;

public class Sync {
	private static final ThreadLocal<Locale> localeThreadLocal = new NamedThreadLocal<Locale>("locale");
	public static ThreadLocal<Locale> getLocaleThreadLocal() {
		return localeThreadLocal;
	}
}