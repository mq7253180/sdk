package com.quincy.auth.o;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class XSession implements Serializable {
	private static final long serialVersionUID = 997874172809782407L;
	private User user;
	private List<String> roles;
	private List<String> permissions;
	private List<Menu> menus;
	private Map<String, BigDecimal> currencyAccounts;
	private Map<String, Serializable> attributes;
}