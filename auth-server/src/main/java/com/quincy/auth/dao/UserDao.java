package com.quincy.auth.dao;

import com.quincy.auth.entity.UserDto;
import com.quincy.sdk.annotation.jdbc.ExecuteQuery;
import com.quincy.sdk.annotation.jdbc.ExecuteUpdate;
import com.quincy.sdk.annotation.jdbc.JDBCDao;

@JDBCDao
public interface UserDao {
	@ExecuteQuery(sql = "SELECT * FROM b_user WHERE id=?", returnItemType = UserDto.class)
	public UserDto find(Long id);
	@ExecuteUpdate(sql = "INSERT INTO b_user(id, username, name, gender, password, mobile_phone, email, avatar) VALUES(?, ?, ?, ?, ?, ?, ?, ?)")
	public int save(Long id, String username, String name, Byte gender, String password, String mobilePhone, String email, String avatar);
	@ExecuteUpdate(sql = "UPDATE b_user SET name=?,username=?,gender=?,password=?,mobile_phone=?,email=?,avatar=?,jsessionid_pc_browser=?,jsessionid_mobile_browser=?,jsessionid_app=? WHERE id=?")
	public int update(String name, String username, Byte gender, String password, String mobilePhone, String email, String avatar, String jsessionidPcBrowser, String jsessionidMobileBrowser, String jsessionidApp, Long id);
	@ExecuteUpdate(sql = "UPDATE b_user SET password=? WHERE id=?")
	public int updatePassword(String password, Long id);
	@ExecuteUpdate(sql = "UPDATE b_user SET jsessionid_pc_browser=? WHERE id=?")
	public int updateJsessionidPcBrowser(String jsessionid, Long id);
	@ExecuteUpdate(sql = "UPDATE b_user SET jsessionid_mobile_browser=? WHERE id=?")
	public int updateJsessionidMobileBrowser(String jsessionid, Long id);
	@ExecuteUpdate(sql = "UPDATE b_user SET jsessionid_app=? WHERE id=?")
	public int updateJsessionidApp(String jsessionid, Long id);
}