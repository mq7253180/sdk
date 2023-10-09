package com.quincy.core;

import java.util.Collection;

import lombok.Data;

@Data
public class DBConnPoolParams {
	private boolean poolingStatements;
	private int maxOpenPreparedStatements;
	private int initialSize;
	private Integer defaultQueryTimeoutSeconds;
	private String validationQuery;
	private int validationQueryTimeoutSeconds;
	private long maxConnLifetimeMillis;
	private Collection<String> connectionInitSqls;
	private boolean logExpiredConnections;
	private boolean cacheState;
	private int defaultTransactionIsolation;
	private String connectionProperties;
	private boolean fastFailValidation;
	private Collection<String> disconnectionSqlCodes;
	private String defaultCatalog;
	private boolean accessToUnderlyingConnectionAllowed;
}