package com.quincy.sdk;

import java.io.Serializable;

public class DynamicField implements Serializable {
	private static final long serialVersionUID = -4784892811403682316L;
	private Integer id;
	private String name;
	private String align = "left";
	private int sort;

	public DynamicField() {
		
	}
	public DynamicField(Integer id, String name, String align, int sort) {
		this.id = id;
		this.name = name;
		if(align!=null)
			this.align = align;
		this.sort = sort;
	}
	public Integer getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getAlign() {
		return align;
	}
	public int getSort() {
		return sort;
	}
}