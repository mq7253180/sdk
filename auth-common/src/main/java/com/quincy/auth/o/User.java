package com.quincy.auth.o;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class User implements Serializable {
	private static final long serialVersionUID = 3068671906589197352L;
	private Long id;
	private String name;
	private String nickName;
	private String username;
	private String email;
	private String mobilePhone;
	private String password;
	private Date creationTime;
	private Date lastLogined;
	private String jsessionid;
}