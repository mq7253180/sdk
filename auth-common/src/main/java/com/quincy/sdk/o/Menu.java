package com.quincy.sdk.o;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class Menu implements Serializable {
	private static final long serialVersionUID = 6829594433533198471L;
	private Long id;
	private Long pId;
	private String name;
	private String uri;
	private String icon;
	private List<Menu> children;
}