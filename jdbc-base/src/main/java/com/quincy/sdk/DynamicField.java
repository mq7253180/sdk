package com.quincy.sdk;

import java.io.Serializable;

public class DynamicField implements Serializable {
	private static final long serialVersionUID = -5958162058880251653L;
	private Integer id;
	private String name;
	private int sort;

	public DynamicField() {
		
	}
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