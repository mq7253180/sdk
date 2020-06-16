package com.quincy.auth.o;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class User implements Serializable {
	private static final long serialVersionUID = 3068671906589197352L;
	private Long id;
	private Date creationTime;
	private String username;
	private String name;
	private String password;
	private String email;
	private String mobilePhone;
	private String jsessionid;
	private Date lastLogined;
}