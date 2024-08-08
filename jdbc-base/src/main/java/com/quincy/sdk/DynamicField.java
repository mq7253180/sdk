package com.quincy.sdk;

public class DynamicField {
	private String name;
	private Object value;

	public DynamicField(String name, Object value) {
		this.name = name;
		this.value = value;
	}
	public String getName() {
		return name;
	}
	public Object getValue() {
		return value;
	}
}