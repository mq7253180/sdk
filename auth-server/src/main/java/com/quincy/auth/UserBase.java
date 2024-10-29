package com.quincy.auth;

import java.util.Date;

public interface UserBase {
	public Long getId();
	public void setId(Long id);
	public Date getCreationTime();
	public void setCreationTime(Date creationTime);
	public String getUsername();
	public void setUsername(String username);
	public String getName();
	public void setName(String name);
	public Byte getGender();
	public void setGender(Byte gender);
	public String getPassword();
	public void setPassword(String password);
	public String getEmail();
	public void setEmail(String email);
	public String getMobilePhone();
	public void setMobilePhone(String mobilePhone);
	public String getAvatar();
	public void setAvatar(String avatar);
	public String getJsessionidPcBrowser();
	public void setJsessionidPcBrowser(String jsessionidPcBrowser);
	public String getJsessionidMobileBrowser();
	public void setJsessionidMobileBrowser(String jsessionidMobileBrowser);
	public String getJsessionidApp();
	public void setJsessionidApp(String jsessionidApp);
}