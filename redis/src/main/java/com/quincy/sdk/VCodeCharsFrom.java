package com.quincy.sdk;

public enum VCodeCharsFrom {
	DIGITS("0123456789"), MIXED("23456789abcdefghijkmnpqrstuvwxyzABCDEFGHLJKMNPQRSTUVWXYZ");

	private String value;

	private VCodeCharsFrom(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}