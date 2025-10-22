package com.quincy.sdk;

public class SelfException extends RuntimeException {
	private static final long serialVersionUID = -4979927675359782691L;

	public SelfException(String message) {
        super(message);
    }
}