package com.quincy.sdk;

public class DynamicField {
	private Integer id;
	private String name;
	private int sort;

	public DynamicField(Integer id, String name, int sort) {
		this.id = id;
		this.name = name;
		this.sort = sort;
	}
	public Integer getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public int getSort() {
		return sort;
	}
}