package com.quincy.auth.dao;

import com.quincy.auth.entity.UserDto;
import com.quincy.sdk.annotation.jdbc.ExecuteQuery;
import com.quincy.sdk.annotation.jdbc.JDBCDao;

@JDBCDao
public interface UserDao {
	@ExecuteQuery(sql = "SELECT * FROM b_user WHERE id=?", returnItemType = UserDto.class)
	public UserDto find(Long id);
}