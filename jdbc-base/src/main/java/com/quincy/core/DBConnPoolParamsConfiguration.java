package com.quincy.core;

import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.sdk.helper.CommonHelper;

@Configuration
public class DBConnPoolParamsConfiguration {
	@Value("${spring.datasource.dbcp2.poolPreparedStatements:#{null}}")
	private Boolean poolPreparedStatements;
	@Value("${spring.datasource.dbcp2.maxOpenPreparedStatements:#{null}}")
	private Integer maxOpenPreparedStatements;
	@Value("${spring.datasource.dbcp2.initialSize:#{null}}")
	private Integer initialSize;
	@Value("${spring.datasource.dbcp2.defaultQueryTimeoutSeconds:#{null}}")
	private Integer defaultQueryTimeoutSeconds;
	@Value("${spring.datasource.dbcp2.validationQuery:#{null}}")
	private String validationQuery;
	@Value("${spring.datasource.dbcp2.validationQueryTimeoutSeconds:#{null}}")
	private Integer validationQueryTimeoutSeconds;
	@Value("${spring.datasource.dbcp2.maxConnLifetimeMillis:#{null}}")
	private Long maxConnLifetimeMillis;
	@Value("${spring.datasource.dbcp2.logExpiredConnections:#{null}}")
	private Boolean logExpiredConnections;
	@Value("${spring.datasource.dbcp2.cacheState:#{null}}")
	private Boolean cacheState;
	@Value("${spring.datasource.dbcp2.connectionInitSqls:#{null}}")
	private String _connectionInitSqls;
	@Value("${spring.datasource.dbcp2.defaultTransactionIsolation:#{null}}")
	private Integer defaultTransactionIsolation;
	@Value("${spring.datasource.dbcp2.connectionProperties:#{null}}")
	private String connectionProperties;
	@Value("${spring.datasource.dbcp2.fastFailValidation:#{null}}")
	private Boolean fastFailValidation;
	@Value("${spring.datasource.dbcp2.disconnectionSqlCodes:#{null}}")
	private String _disconnectionSqlCodes;
	@Value("${spring.datasource.dbcp2.defaultCatalog}")
	private String defaultCatalog;
	@Value("${spring.datasource.dbcp2.accessToUnderlyingConnectionAllowed:#{null}}")
	private Boolean accessToUnderlyingConnectionAllowed;

	@Bean
	public DBConnPoolParams dbConnPoolParams() throws SQLException {
		BasicDataSource ds = new BasicDataSource();
		DBConnPoolParams p = new DBConnPoolParams();
		p.setPoolingStatements(poolPreparedStatements==null?ds.isPoolPreparedStatements():poolPreparedStatements);
		p.setMaxOpenPreparedStatements(maxOpenPreparedStatements==null?ds.getMaxOpenPreparedStatements():maxOpenPreparedStatements);
		p.setInitialSize(initialSize==null?ds.getInitialSize():initialSize);
		p.setDefaultQueryTimeoutSeconds(defaultQueryTimeoutSeconds==null?ds.getDefaultQueryTimeout():defaultQueryTimeoutSeconds);
		p.setValidationQuery(validationQuery==null?ds.getValidationQuery():CommonHelper.trim(validationQuery));
		p.setValidationQueryTimeoutSeconds(validationQueryTimeoutSeconds==null?ds.getValidationQueryTimeout():validationQueryTimeoutSeconds);
		p.setMaxConnLifetimeMillis(maxConnLifetimeMillis==null?ds.getMaxConnLifetimeMillis():maxConnLifetimeMillis);
		p.setLogExpiredConnections(logExpiredConnections==null?ds.getLogExpiredConnections():logExpiredConnections);
		p.setCacheState(cacheState==null?ds.getCacheState():cacheState);
		p.setDefaultTransactionIsolation(defaultTransactionIsolation==null?ds.getDefaultTransactionIsolation():defaultTransactionIsolation);
		p.setConnectionProperties(connectionProperties);
		p.setFastFailValidation(fastFailValidation==null?ds.getFastFailValidation():fastFailValidation);
		p.setDefaultCatalog(defaultCatalog);
		p.setAccessToUnderlyingConnectionAllowed(accessToUnderlyingConnectionAllowed==null?ds.isAccessToUnderlyingConnectionAllowed():accessToUnderlyingConnectionAllowed);
		if(_connectionInitSqls!=null) {
			String[] connectionInitSqls = _connectionInitSqls.split(";");
			if(connectionInitSqls!=null&&connectionInitSqls.length>0)
				p.setConnectionInitSqls(Arrays.asList(connectionInitSqls));
		}
		if(_disconnectionSqlCodes!=null) {
			String[] disconnectionSqlCodes = _disconnectionSqlCodes.split(",");
			if(disconnectionSqlCodes!=null&&disconnectionSqlCodes.length>0)
				p.setDisconnectionSqlCodes(Arrays.asList(disconnectionSqlCodes));
		}
		ds.close();
		return p;
	}
}