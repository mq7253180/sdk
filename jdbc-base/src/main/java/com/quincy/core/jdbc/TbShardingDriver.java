package com.quincy.core.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class TbShardingDriver implements Driver {
	static {
        try {
            DriverManager.registerDriver(new TbShardingDriver());
        } catch (SQLException E) {
            throw new RuntimeException("Can't register driver!");
        } catch (Exception E) {
        	E.printStackTrace();
        	throw new RuntimeException("newInstance error!");
        }
    }
	public final static String URL_PREFIX = "tbsharding:";
	private Driver originalDriver;

	public TbShardingDriver() throws SQLException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        // Required for Class.forName().newInstance()
//		System.out.println("---TbShardingDriver()");
		this.originalDriver = OriginalDriverClassHolder.CLASS.getDeclaredConstructor(new Class[] {}).newInstance();
	}

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		if(url.startsWith(URL_PREFIX)) {
			Connection originalConnection = this.originalDriver.connect(url.replaceFirst(URL_PREFIX, ""), info);
			return originalConnection==null?null:new TbShardingConnection(originalConnection);
		}
		return null;
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		if(url.startsWith(URL_PREFIX)) {
			return this.originalDriver.acceptsURL(url.replaceFirst(URL_PREFIX, ""));
		}
		return false;
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return this.originalDriver.getPropertyInfo(url, info);
	}

	@Override
	public int getMajorVersion() {
		return this.originalDriver.getMajorVersion();
	}

	@Override
	public int getMinorVersion() {
		return this.originalDriver.getMinorVersion();
	}

	@Override
	public boolean jdbcCompliant() {
		return this.originalDriver.jdbcCompliant();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return this.originalDriver.getParentLogger();
	}
}
