package com.quincy.auth.dao;

import com.quincy.auth.entity.LoginUserMapping;
import com.quincy.sdk.annotation.jdbc.ExecuteQuery;
import com.quincy.sdk.annotation.jdbc.ExecuteUpdate;
import com.quincy.sdk.annotation.jdbc.JDBCDao;

@JDBCDao
public interface LoginUserMappingDao {
	@ExecuteQuery(sql = "SELECT * FROM b_login_user_mapping WHERE login_name=?", returnItemType = LoginUserMapping.class)
	public LoginUserMapping findByLoginName(String loginName);
	@ExecuteUpdate(sql = "INSERT INTO b_login_user_mapping(login_name, user_id) VALUES(?, ?)")
	public int save(String loginName, Long userId);
	@ExecuteUpdate(sql = "UPDATE b_login_user_mapping SET login_name=? WHERE id=?")
	public int updateLoginName(String loginName, Long id);
	@ExecuteUpdate(sql = "DELETE FROM b_login_user_mapping WHERE login_name=?")
	public int deleteByLoginName(String loginName);
}