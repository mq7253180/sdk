package com.quincy.sdk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public interface AllShardsDaoSupport {
	public Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, MasterOrSlave masterOrSlave, Object... args) throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException;
	public int[] executeUpdate(String sql, MasterOrSlave masterOrSlave, Object... args) throws SQLException;
}