package com.quincy.sdk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public interface ShardingJdbcDao {
	public Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, MasterOrSlave masterOrSlave, Object... args) throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, InterruptedException, ExecutionException;
	public int[] executeUpdate(String sql, MasterOrSlave masterOrSlave, Object... args) throws SQLException, InterruptedException, ExecutionException;
	public Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, MasterOrSlave masterOrSlave, boolean anyway, Object... args) throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, InterruptedException, ExecutionException;
	public int[] executeUpdate(String sql, MasterOrSlave masterOrSlave, boolean anyway, long shardingKeyToSkip, Object... args) throws SQLException, InterruptedException, ExecutionException;
}