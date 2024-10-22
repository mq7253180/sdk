package com.quincy.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class SecureTrueRequestWrapper extends HttpServletRequestWrapper {
	public SecureTrueRequestWrapper(HttpServletRequest request) {
		super(request);
	}

	public boolean isSecure() {
		return true;
	}
}
