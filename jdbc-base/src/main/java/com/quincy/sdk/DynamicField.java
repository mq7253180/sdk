package com.quincy.sdk;

import java.io.Serializable;

public class DynamicField implements Serializable {
	private static final long serialVersionUID = -4784892811403682316L;
	private Integer id;
	private String name;
	private String clazz = "left";
	private int sort;

	public DynamicField() {
		
	}
	public DynamicField(Integer id, String name, String clazz, int sort) {
		this.id = id;
		this.name = name;
		if(clazz!=null)
			this.clazz = clazz;
		this.sort = sort;
	}
	public Integer getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getClazz() {
		return clazz;
	}
	public int getSort() {
		return sort;
	}
}