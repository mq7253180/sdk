package com.quincy.core.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class TbShardingConnection implements Connection {
	private static String[] TABLE_NAMES = null;
	private final static ThreadLocal<Integer> SHARD = new ThreadLocal<Integer>();
	private Connection original;

	public TbShardingConnection(Connection original) {
		this.original = original;
	}

	public static void setTableNames(String... tableNames) {
		TABLE_NAMES = tableNames;
	}

	public static void setShard(int shard) {
		SHARD.set(shard);
	}

	private String relpaceAll(String _sql) {
		if(TABLE_NAMES==null) {
			throw new RuntimeException("Please set table names!");
		} else {
			String sql = _sql;
			for(String tableName:TABLE_NAMES) {
				sql = sql.replaceAll("\\b"+tableName+"\\b", tableName+"_"+SHARD.get());
			}
//			System.out.println("TB_SHARDING_SQL: "+sql);
			return sql;
		}
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return this.original.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return this.original.isWrapperFor(iface);
	}

	@Override
	public Statement createStatement() throws SQLException {
		return this.original.createStatement();
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return this.original.prepareStatement(this.relpaceAll(sql));
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		return this.original.prepareCall(this.relpaceAll(sql));
	}

	@Override
	public String nativeSQL(String sql) throws SQLException {
		return this.original.nativeSQL(this.relpaceAll(sql));
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		this.original.setAutoCommit(autoCommit);
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		return this.original.getAutoCommit();
	}

	@Override
	public void commit() throws SQLException {
		this.original.commit();
	}

	@Override
	public void rollback() throws SQLException {
		this.original.rollback();
	}

	@Override
	public void close() throws SQLException {
		this.original.close();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return this.original.isClosed();
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return this.original.getMetaData();
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		this.original.setReadOnly(readOnly);
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return this.original.isReadOnly();
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
		this.original.setCatalog(catalog);
	}

	@Override
	public String getCatalog() throws SQLException {
		return this.original.getCatalog();
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		this.original.setTransactionIsolation(level);
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		return this.original.getTransactionIsolation();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return this.original.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException {
		this.original.clearWarnings();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return this.original.createStatement(resultSetType, resultSetConcurrency);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return this.original.prepareStatement(this.relpaceAll(sql), resultSetType, resultSetConcurrency);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return this.original.prepareCall(this.relpaceAll(sql), resultSetType, resultSetConcurrency);
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return this.original.getTypeMap();
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		this.original.setTypeMap(map);
	}

	@Override
	public void setHoldability(int holdability) throws SQLException {
		this.original.setHoldability(holdability);
	}

	@Override
	public int getHoldability() throws SQLException {
		return this.original.getHoldability();
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		return this.original.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		return this.original.setSavepoint(name);
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		this.original.rollback(savepoint);
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		this.original.releaseSavepoint(savepoint);
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return this.original.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return this.original.prepareStatement(this.relpaceAll(sql), resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return this.original.prepareCall(this.relpaceAll(sql), resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return this.original.prepareStatement(this.relpaceAll(sql), autoGeneratedKeys);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return this.original.prepareStatement(this.relpaceAll(sql), columnIndexes);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return this.original.prepareStatement(this.relpaceAll(sql), columnNames);
	}

	@Override
	public Clob createClob() throws SQLException {
		return this.original.createClob();
	}

	@Override
	public Blob createBlob() throws SQLException {
		return this.original.createBlob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		return this.original.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		return this.original.createSQLXML();
	}

	@Override
	public boolean isValid(int timeout) throws SQLException {
		return this.original.isValid(timeout);
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		this.original.setClientInfo(name, value);
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		this.original.setClientInfo(properties);
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		return this.original.getClientInfo(name);
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		return this.original.getClientInfo();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return this.original.createArrayOf(typeName, elements);
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return this.original.createStruct(typeName, attributes);
	}

	@Override
	public void setSchema(String schema) throws SQLException {
		this.original.setSchema(schema);
	}

	@Override
	public String getSchema() throws SQLException {
		return this.original.getSchema();
	}

	@Override
	public void abort(Executor executor) throws SQLException {
		this.original.abort(executor);
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		this.original.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		return this.original.getNetworkTimeout();
	}
}
