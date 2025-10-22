package com.quincy.sdk.entity;

import java.io.Serializable;

import com.quincy.sdk.annotation.jdbc.Column;
import com.quincy.sdk.annotation.jdbc.DTO;

@DTO
public class Region implements Serializable {
	private static final long serialVersionUID = -7160794018694023343L;
	@Column("id")
	private Long id;
	@Column("en_name")
	private String enName;
	@Column("cn_name")
	private String cnName;
	@Column("tel_prefix")
	private String telPrefix;
	@Column("code")
	private String code;
	@Column("code2")
	private String code2;
	@Column("currency")
	private String currency;
	@Column("locale")
	private String locale;
	@Column("parent_id")
	private Long parentId;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getEnName() {
		return enName;
	}
	public void setEnName(String enName) {
		this.enName = enName;
	}
	public String getCnName() {
		return cnName;
	}
	public void setCnName(String cnName) {
		this.cnName = cnName;
	}
	public String getTelPrefix() {
		return telPrefix;
	}
	public void setTelPrefix(String telPrefix) {
		this.telPrefix = telPrefix;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getCode2() {
		return code2;
	}
	public void setCode2(String code2) {
		this.code2 = code2;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public Long getParentId() {
		return parentId;
	}
	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}
}