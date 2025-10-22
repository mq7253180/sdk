package com.quincy.auth.entity;

import java.io.Serializable;
import java.util.Date;

import com.quincy.sdk.annotation.jdbc.Column;
import com.quincy.sdk.annotation.jdbc.DTO;

@DTO
public class UserDto implements Serializable {
	private static final long serialVersionUID = -5718146733023935649L;
	@Column("id")
	private Long id;
	@Column("username")
	private String username;
	@Column("email")
	private String email;
	@Column("mobile_phone")
	private String mobilePhone;
	@Column("password")
	private String password;
	@Column("creation_time")
	private Date creationTime;
	@Column("jsessionid_pc_browser")
	private String pcBrowserJsessionid;
	@Column("jsessionid_mobile_browser")
	private String mobileBrowserJsessionid;
	@Column("jsessionid_app")
	private String appJsessionid;
	@Column("name")
	private String name;
	@Column("gender")
	private Byte gender;
	@Column("avatar")
	private String avatar;
	@Column("jsessionid_pc_browser")
	private String jsessionidPcBrowser;
	@Column("jsessionid_mobile_browser")
	private String jsessionidMobileBrowser;
	@Column("jsessionid_app")
	private String jsessionidApp;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getMobilePhone() {
		return mobilePhone;
	}
	public void setMobilePhone(String mobilePhone) {
		this.mobilePhone = mobilePhone;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Date getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}
	public String getPcBrowserJsessionid() {
		return pcBrowserJsessionid;
	}
	public void setPcBrowserJsessionid(String pcBrowserJsessionid) {
		this.pcBrowserJsessionid = pcBrowserJsessionid;
	}
	public String getMobileBrowserJsessionid() {
		return mobileBrowserJsessionid;
	}
	public void setMobileBrowserJsessionid(String mobileBrowserJsessionid) {
		this.mobileBrowserJsessionid = mobileBrowserJsessionid;
	}
	public String getAppJsessionid() {
		return appJsessionid;
	}
	public void setAppJsessionid(String appJsessionid) {
		this.appJsessionid = appJsessionid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Byte getGender() {
		return gender;
	}
	public void setGender(Byte gender) {
		this.gender = gender;
	}
	public String getAvatar() {
		return avatar;
	}
	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}
	public String getJsessionidPcBrowser() {
		return jsessionidPcBrowser;
	}
	public void setJsessionidPcBrowser(String jsessionidPcBrowser) {
		this.jsessionidPcBrowser = jsessionidPcBrowser;
	}
	public String getJsessionidMobileBrowser() {
		return jsessionidMobileBrowser;
	}
	public void setJsessionidMobileBrowser(String jsessionidMobileBrowser) {
		this.jsessionidMobileBrowser = jsessionidMobileBrowser;
	}
	public String getJsessionidApp() {
		return jsessionidApp;
	}
	public void setJsessionidApp(String jsessionidApp) {
		this.jsessionidApp = jsessionidApp;
	}
}