package com.quincy.sdk.o;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class XSession implements Serializable {
	private static final long serialVersionUID = 997874172809782407L;
	private User user;
	private List<String> roles;
	private List<String> permissions;
	private List<Menu> menus;
}