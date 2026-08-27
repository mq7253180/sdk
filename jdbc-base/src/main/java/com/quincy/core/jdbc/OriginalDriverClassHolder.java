package com.quincy.core.jdbc;

public class OriginalDriverClassHolder {
	protected static Class<? extends java.sql.Driver> CLASS;

	public static void set(Class<? extends java.sql.Driver> driverClass) {
		CLASS = driverClass;
	}
}
