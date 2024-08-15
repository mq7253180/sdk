package com.quincy.auth.o;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import lombok.Data;

@Data
public class User implements Serializable {
	private static final long serialVersionUID = 3068671906589197352L;
	private Long id;
	private Long enterpriseId;
	private String username;
	private String email;
	private String mobilePhone;
	private String password;
	private Date creationTime;
	private Date lastLogined;
	private String jsessionid;
	private String name;
	private String lastName;
	private String firstName;
	private String nickName;
	private Map<String, BigDecimal> currencyAccounts;
	private Map<String, Serializable> attributes;
}