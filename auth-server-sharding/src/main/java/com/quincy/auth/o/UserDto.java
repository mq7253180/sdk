package com.quincy.auth.o;

import java.util.Date;

import com.quincy.auth.UserBase;
import com.quincy.sdk.annotation.jdbc.Column;
import com.quincy.sdk.annotation.jdbc.DTO;

import lombok.Data;

@DTO
@Data
public class UserDto implements UserBase {
	@Column("id")
	private Long id;
	@Column("creation_time")
	private Date creationTime;
	@Column("username")
	private String username;
	@Column("name")
	private String name;
	@Column("gender")
	private Byte gender;
	@Column("password")
	private String password;
	@Column("email")
	private String email;
	@Column("mobile_phone")
	private String mobilePhone;
	@Column("avatar")
	private String avatar;
	@Column("jsessionid_pc_browser")
	private String jsessionidPcBrowser;
	@Column("jsessionid_mobile_browser")
	private String jsessionidMobileBrowser;
	@Column("jsessionid_app")
	private String jsessionidApp;
}