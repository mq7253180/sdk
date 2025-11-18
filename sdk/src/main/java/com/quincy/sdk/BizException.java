package com.quincy.sdk;

public class BizException extends RuntimeException {
	private static final long serialVersionUID = -4979927675359782691L;

	public BizException(String message) {
        super(message);
    }
}