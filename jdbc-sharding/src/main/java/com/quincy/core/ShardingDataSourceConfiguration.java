package com.quincy.core;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import com.quincy.core.db.RoutingDataSource;
import com.quincy.sdk.MasterOrSlave;

@Configuration
public class ShardingDataSourceConfiguration {
	@Value("${spring.datasource.driver-class-name}")
	private String driverClassName;
	@Value("${spring.datasource.username}")
	private String userName;
	@Value("${spring.datasource.password}")
	private String password;
	@Value("${spring.datasource.pool.masterRatio}")
	private int masterRatio;
	@Value("${spring.datasource.sharding.url.prefix}")
	private String urlPrefix;
	@Value("${spring.datasource.sharding.url.suffix}")
	private String urlSuffix;
	@Value("${spring.datasource.sharding.count}")
	private int shardingCount;
	@Autowired
	private DataSourceFactory dataSourceFactory;
	@Autowired
	private ApplicationContext applicationContext;
	private final static String PROPERTY_KEY_PREFIX = "spring.datasource.sharding.";

	@Bean(name = "dataSource")
    public DataSource routingDataSource() throws SQLException {
		Map<Object, Object> targetDataSources = new HashMap<Object, Object>(shardingCount*2);
		for(int i=0;i<shardingCount;i++) {
			String masterKey = PROPERTY_KEY_PREFIX+i+".master";
			String masterVal = applicationContext.getEnvironment().getProperty(masterKey);
			Assert.hasText(masterVal, "第"+i+"个分片写库没有设置(从0开始)");
			String slaveKey = PROPERTY_KEY_PREFIX+i+".slave";
			String slaveVal = applicationContext.getEnvironment().getProperty(slaveKey);
			Assert.hasText(slaveVal, "第"+i+"个分片只读库没有设置(从0开始)");
			BasicDataSource masterDB = dataSourceFactory.create(1);
			masterDB.setDriverClassName(driverClassName);
			masterDB.setUrl(urlPrefix+masterVal+urlSuffix);
			masterDB.setUsername(userName);
			masterDB.setPassword(password);
			masterDB.setDefaultAutoCommit(true);
//			masterDB.setAutoCommitOnReturn(true);
			masterDB.setRollbackOnReturn(false);
			masterDB.setDefaultReadOnly(false);

			BasicDataSource slaveDB = dataSourceFactory.create(masterRatio);
			slaveDB.setDriverClassName(driverClassName);
			slaveDB.setUrl(urlPrefix+slaveVal+urlSuffix);
			slaveDB.setUsername(userName);
			slaveDB.setPassword(password);
			slaveDB.setDefaultAutoCommit(false);
//			slaveDB.setAutoCommitOnReturn(false);
			slaveDB.setRollbackOnReturn(true);
			slaveDB.setDefaultReadOnly(true);

			targetDataSources.put(MasterOrSlave.MASTER.value()+i, masterDB);
			targetDataSources.put(MasterOrSlave.SLAVE.value()+i, slaveDB);
		}
		RoutingDataSource db = new RoutingDataSource();
		db.setTargetDataSources(targetDataSources);
		db.setDefaultTargetDataSource(targetDataSources.get(MasterOrSlave.MASTER.value()+0));
		return db;
	}
}