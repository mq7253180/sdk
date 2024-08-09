package com.quincy.sdk;

public class DynamicField {
	private String name;
	private Object value;
	private int sort;

	public DynamicField(String name, Object value, int sort) {
		this.name = name;
		this.value = value;
		this.sort = sort;
	}
	public String getName() {
		return name;
	}
	public Object getValue() {
		return value;
	}
	public int getSort() {
		return sort;
	}
}