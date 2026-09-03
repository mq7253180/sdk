package com.quincy.core;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quincy.sdk.MasterOrSlave;
import com.quincy.core.db.RoutingDataSource;

@Configuration
//@AutoConfigureAfter(CommonApplicationContext.class)
//@Import(CommonApplicationContext.class)
public class DataSourceConfiguration {//implements TransactionManagementConfigurer {
	@Value("${spring.datasource.driver-class-name}")
	private String driverClassName;
	@Value("${spring.datasource.url}")
	private String masterUrl;
	@Value("${spring.datasource.username}")
	private String masterUserName;
	@Value("${spring.datasource.password}")
	private String masterPassword;
	@Value("${spring.datasource.url.slave}")
	private String slaveUrl;
	@Value("${spring.datasource.username.slave}")
	private String slaveUserName;
	@Value("${spring.datasource.password.slave}")
	private String slavePassword;
	@Value("${spring.datasource.pool.masterRatio}")
	private int masterRatio;
	@Autowired
	private DataSourceFactory dataSourceFactory;

	@Bean(name = "dataSource")
    public DataSource routingDataSource() throws SQLException {
		BasicDataSource masterDB = dataSourceFactory.create(1);
		masterDB.setDriverClassName(driverClassName);
		masterDB.setUrl(masterUrl);
		masterDB.setUsername(masterUserName);
		masterDB.setPassword(masterPassword);
		masterDB.setDefaultAutoCommit(true);
//		masterDB.setAutoCommitOnReturn(true);
		masterDB.setRollbackOnReturn(false);
		masterDB.setDefaultReadOnly(false);

		BasicDataSource slaveDB = dataSourceFactory.create(masterRatio);
		slaveDB.setDriverClassName(driverClassName);
		slaveDB.setUrl(slaveUrl);
		slaveDB.setUsername(slaveUserName);
		slaveDB.setPassword(slavePassword);
		slaveDB.setDefaultAutoCommit(false);
//		slaveDB.setAutoCommitOnReturn(false);
		slaveDB.setRollbackOnReturn(true);
		slaveDB.setDefaultReadOnly(true);

		Map<Object, Object> targetDataSources = new HashMap<Object, Object>(2);
		targetDataSources.put(MasterOrSlave.MASTER.value(), masterDB);
		targetDataSources.put(MasterOrSlave.SLAVE.value(), slaveDB);
		RoutingDataSource db = new RoutingDataSource();
		db.setTargetDataSources(targetDataSources);
		db.setDefaultTargetDataSource(masterDB);
		return db;
	}
}