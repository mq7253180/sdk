package com.quincy.auth.dao;

import com.quincy.auth.o.UserDto;
import com.quincy.sdk.annotation.jdbc.ExecuteQuery;
import com.quincy.sdk.annotation.jdbc.JDBCDao;

@JDBCDao
public interface UserDao {
	@ExecuteQuery(sql = "SELECT * FROM b_user WHERE id=?", returnItemType = UserDto.class, newConn = true)
	public UserDto find(Long id);
}