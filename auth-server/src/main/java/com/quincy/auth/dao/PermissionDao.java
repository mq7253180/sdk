package com.quincy.auth.dao;

import java.util.List;

import com.quincy.auth.entity.Permission;
import com.quincy.sdk.annotation.jdbc.ExecuteQuery;
import com.quincy.sdk.annotation.jdbc.JDBCDao;

@JDBCDao
public interface PermissionDao {
	@ExecuteQuery(sql = "SELECT * FROM s_permission", returnItemType = Permission.class)
	public List<Permission> findAll();
}