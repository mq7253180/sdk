package com.quincy.sdk.o;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class User implements Serializable {
	private static final long serialVersionUID = 3068671906589197352L;
	private Long id;
	private Long shardingKey;
	private String username;
	private String email;
	private String mobilePhone;
	private String password;
	private Date creationTime;
//	private Date lastLogined;
	private String jsessionid;
	private String name;
	private String lastName;
	private String firstName;
	private String nickName;
	private Byte gender;
	private String avatar;
	private List<String> roles;
	private List<String> permissions;
	private List<Menu> menus;
	private Enterprise currentEnterprise;//当前已选租户
	private List<Enterprise> enterprises;//多租户
	private Map<String, BigDecimal> currencyAccounts;
}