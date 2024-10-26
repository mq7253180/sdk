package com.quincy.sdk;

import java.io.Serializable;

public class DynamicColumn extends DynamicField implements Cloneable, Serializable {
	private static final long serialVersionUID = 7905601732483586056L;
	private Object value;

	public DynamicColumn() {
		super();
	}
	public DynamicColumn(Integer id, String name, String clazz, int sort) {
		super(id, name, clazz, sort);
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