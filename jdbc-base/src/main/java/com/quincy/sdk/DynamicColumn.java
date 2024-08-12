package com.quincy.sdk;

public class DynamicColumn implements Cloneable {
	private Integer id;
	private String name;
	private Object value;
	private int sort;

	public DynamicColumn(Integer id, String name, int sort) {
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
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}

	@Override
    public DynamicColumn clone() throws CloneNotSupportedException {
		return (DynamicColumn)super.clone();
	}
}