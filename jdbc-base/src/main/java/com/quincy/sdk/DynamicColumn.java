package com.quincy.sdk;

public class DynamicColumn {
	private String name;
	private Object value;
	private int sort;

	public DynamicColumn(String name, Object value, int sort) {
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