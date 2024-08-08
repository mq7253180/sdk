package com.quincy.sdk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public interface JdbcDao {
	public Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, Object... args) throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException;
	public Object executeQueryWithDynamicFields(String sql, String tableName, Class<?> returnType, Class<?> returnItemType, Object... args) throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException;
	public int executeUpdate(String sql, Object... args) throws SQLException;
	public int executeUpdateWithHistory(String sql, Object... args) throws SQLException;
	public int executeUpdateWithHistory(String sql, String selectSql, Object... args) throws SQLException;
}