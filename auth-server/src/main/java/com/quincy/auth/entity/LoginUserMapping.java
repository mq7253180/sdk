package com.quincy.auth.entity;

import java.io.Serializable;

import com.quincy.sdk.annotation.jdbc.Column;
import com.quincy.sdk.annotation.jdbc.DTO;

@DTO
public class LoginUserMapping implements Serializable {
	private static final long serialVersionUID = -8013534584779089565L;
	@Column("id")
	private Long id;
	@Column("login_name")
	private String loginName;
	@Column("user_id")
	private Long userId;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getLoginName() {
		return loginName;
	}
	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
}