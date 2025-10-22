package com.quincy.core.dao;

import com.quincy.sdk.annotation.jdbc.ExecuteQuery;
import com.quincy.sdk.annotation.jdbc.JDBCDao;

@JDBCDao
public interface UtilsDao {
	@ExecuteQuery(sql = "SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE table_name=?", returnItemType = Long.class)
	public Long selectAutoIncreament(String name);
	@ExecuteQuery(sql = "SELECT LAST_INSERT_ID()", returnItemType = Long.class)
	public Long selectLastInsertId();
}