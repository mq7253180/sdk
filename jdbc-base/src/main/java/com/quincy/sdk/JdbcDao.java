package com.quincy.sdk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;

public interface JdbcDao {
	public Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, Object... args) throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException;
	/**
	 * f.name,f.sort,v.value_decimal and primary key as id must be presented in sqlFrontHalf.
	 */
	public Object executeQueryWithDynamicFields(String sqlFronHalf, String tableName, Class<?> returnType, Class<?> returnItemType, Object... args) throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException;
	public List<String> findDynamicFields(String tableName) throws SQLException;
	public int executeUpdate(String sql, Object... args) throws SQLException;
	public int executeUpdateWithHistory(String sql, Object... args) throws SQLException;
	public int executeUpdateWithHistory(String sql, String selectSql, Object... args) throws SQLException;
}