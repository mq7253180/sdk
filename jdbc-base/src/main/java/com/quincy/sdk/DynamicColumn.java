package com.quincy.sdk;

public class DynamicColumn extends DynamicField implements Cloneable {
	private Object value;

	public DynamicColumn(Integer id, String name, int sort) {
		super(id, name, sort);
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