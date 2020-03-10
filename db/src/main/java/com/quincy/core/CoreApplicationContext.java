package com.quincy.core;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.quincy.core.db.DataSourceHolder;
import com.quincy.core.db.RoutingDataSource;
import com.quincy.sdk.helper.CommonHelper;

import lombok.Data;

@PropertySource({"classpath:application-sdk.properties", "classpath:application-sensitiveness.properties"})
@Configuration
//@AutoConfigureAfter(CommonApplicationContext.class)
//@Import(CommonApplicationContext.class)
public class CoreApplicationContext {//implements TransactionManagementConfigurer {
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
	@Resource(name = InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@Bean(name = "dataSource")
    public DataSource routingDataSource() throws SQLException {
		BasicDataSource masterDB = this.createBasicDataSource(1);
		masterDB.setDriverClassName(driverClassName);
		masterDB.setUrl(masterUrl);
		masterDB.setUsername(masterUserName);
		masterDB.setPassword(masterPassword);
		masterDB.setDefaultAutoCommit(true);
//		masterDB.setAutoCommitOnReturn(true);
		masterDB.setRollbackOnReturn(false);
		masterDB.setDefaultReadOnly(false);

		BasicDataSource slaveDB = this.createBasicDataSource(masterRatio);
		slaveDB.setDriverClassName(driverClassName);
		slaveDB.setUrl(slaveUrl);
		slaveDB.setUsername(slaveUserName);
		slaveDB.setPassword(slavePassword);
		slaveDB.setDefaultAutoCommit(false);
//		slaveDB.setAutoCommitOnReturn(false);
		slaveDB.setRollbackOnReturn(true);
		slaveDB.setDefaultReadOnly(true);

		Map<Object, Object> targetDataSources = new HashMap<Object, Object>(2);
		targetDataSources.put(DataSourceHolder.MASTER, masterDB);
		targetDataSources.put(DataSourceHolder.SLAVE, slaveDB);
		RoutingDataSource db = new RoutingDataSource();
		db.setTargetDataSources(targetDataSources);
		db.setDefaultTargetDataSource(masterDB);
		return db;
	}

	@Data
	private class DBConnPoolParams {
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

	@Bean
	public DBConnPoolParams dbConnPoolParams() throws SQLException {
		String poolPreparedStatements = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.poolPreparedStatements"));
		String maxOpenPreparedStatements = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.maxOpenPreparedStatements"));
		String initialSize = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.initialSize"));
		String defaultQueryTimeoutSeconds = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.defaultQueryTimeoutSeconds"));
		String validationQuery = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.validationQuery"));
		String validationQueryTimeoutSeconds = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.validationQueryTimeoutSeconds"));
		String maxConnLifetimeMillis = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.maxConnLifetimeMillis"));
		String logExpiredConnections = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.logExpiredConnections"));
		String cacheState = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.cacheState"));
		String _connectionInitSqls = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.connectionInitSqls"));
		String defaultTransactionIsolation = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.defaultTransactionIsolation"));
		String connectionProperties = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.connectionProperties"));
		String fastFailValidation = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.fastFailValidation"));
		String _disconnectionSqlCodes = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.disconnectionSqlCodes"));
		String defaultCatalog = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.defaultCatalog"));
		String accessToUnderlyingConnectionAllowed = CommonHelper.trim(properties.getProperty("spring.datasource.dbcp2.accessToUnderlyingConnectionAllowed"));
		BasicDataSource ds = new BasicDataSource();
		DBConnPoolParams p = new DBConnPoolParams();
		p.setPoolingStatements(poolPreparedStatements==null?ds.isPoolPreparedStatements():Boolean.parseBoolean(poolPreparedStatements));
		p.setMaxOpenPreparedStatements(maxOpenPreparedStatements==null?ds.getMaxOpenPreparedStatements():Integer.parseInt(maxOpenPreparedStatements));
		p.setInitialSize(initialSize==null?ds.getInitialSize():Integer.parseInt(initialSize));
		p.setDefaultQueryTimeoutSeconds(defaultQueryTimeoutSeconds==null?ds.getDefaultQueryTimeout():Integer.valueOf(defaultQueryTimeoutSeconds));
		p.setValidationQuery(validationQuery==null?ds.getValidationQuery():CommonHelper.trim(validationQuery));
		p.setValidationQueryTimeoutSeconds(validationQueryTimeoutSeconds==null?ds.getValidationQueryTimeout():Integer.parseInt(validationQueryTimeoutSeconds));
		p.setMaxConnLifetimeMillis(maxConnLifetimeMillis==null?ds.getMaxConnLifetimeMillis():Long.parseLong(maxConnLifetimeMillis));
		p.setLogExpiredConnections(logExpiredConnections==null?ds.getLogExpiredConnections():Boolean.parseBoolean(logExpiredConnections));
		p.setCacheState(cacheState==null?ds.getCacheState():Boolean.parseBoolean(cacheState));
		p.setDefaultTransactionIsolation(defaultTransactionIsolation==null?ds.getDefaultTransactionIsolation():Integer.parseInt(defaultTransactionIsolation));
		p.setConnectionProperties(connectionProperties);
		p.setFastFailValidation(fastFailValidation==null?ds.getFastFailValidation():Boolean.parseBoolean(fastFailValidation));
		p.setDefaultCatalog(defaultCatalog);
		p.setAccessToUnderlyingConnectionAllowed(accessToUnderlyingConnectionAllowed==null?ds.isAccessToUnderlyingConnectionAllowed():Boolean.parseBoolean(accessToUnderlyingConnectionAllowed));
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

	@Autowired
	private GenericObjectPoolConfig poolCfg;
	@Autowired
	private AbandonedConfig abandonedCfg;
	@Autowired
	private DBConnPoolParams dbConnPoolParams;

	private BasicDataSource createBasicDataSource(int ratio) throws SQLException {
		BasicDataSource ds = new BasicDataSource();
		ds.setMaxTotal(poolCfg.getMaxTotal()>0?poolCfg.getMaxTotal()*ratio:poolCfg.getMaxTotal());
		ds.setMaxIdle(poolCfg.getMaxIdle()>0?poolCfg.getMaxIdle()*ratio:poolCfg.getMaxIdle());
		ds.setMinIdle(poolCfg.getMinIdle()>0?poolCfg.getMinIdle()*ratio:poolCfg.getMinIdle());
		ds.setMaxWaitMillis(poolCfg.getMaxWaitMillis());
		ds.setMinEvictableIdleTimeMillis(poolCfg.getMinEvictableIdleTimeMillis());
		ds.setTimeBetweenEvictionRunsMillis(poolCfg.getTimeBetweenEvictionRunsMillis());
		ds.setNumTestsPerEvictionRun(poolCfg.getNumTestsPerEvictionRun()>0?poolCfg.getNumTestsPerEvictionRun()*ratio:poolCfg.getNumTestsPerEvictionRun());
		ds.setTestOnBorrow(poolCfg.getTestOnBorrow());
		ds.setTestOnCreate(poolCfg.getTestOnCreate());
		ds.setTestOnReturn(poolCfg.getTestOnReturn());
		ds.setTestWhileIdle(poolCfg.getTestWhileIdle());
		ds.setLifo(poolCfg.getLifo());
		ds.setEvictionPolicyClassName(poolCfg.getEvictionPolicyClassName());
		ds.setSoftMinEvictableIdleTimeMillis(poolCfg.getSoftMinEvictableIdleTimeMillis());
		ds.setJmxName(poolCfg.getJmxNameBase());
		ds.setRemoveAbandonedOnMaintenance(abandonedCfg.getRemoveAbandonedOnMaintenance());
		ds.setRemoveAbandonedOnBorrow(abandonedCfg.getRemoveAbandonedOnBorrow());
		ds.setRemoveAbandonedTimeout(abandonedCfg.getRemoveAbandonedTimeout());
		ds.setLogAbandoned(abandonedCfg.getLogAbandoned());
		ds.setAbandonedUsageTracking(abandonedCfg.getUseUsageTracking());

		ds.setInitialSize(dbConnPoolParams.getInitialSize()>0?dbConnPoolParams.getInitialSize()*ratio:dbConnPoolParams.getInitialSize());
		ds.setMaxOpenPreparedStatements(dbConnPoolParams.getMaxOpenPreparedStatements()>0?dbConnPoolParams.getMaxOpenPreparedStatements()*ratio:dbConnPoolParams.getMaxOpenPreparedStatements());
		ds.setPoolPreparedStatements(dbConnPoolParams.isPoolingStatements());
		ds.setDefaultQueryTimeout(dbConnPoolParams.getDefaultQueryTimeoutSeconds());
		ds.setValidationQuery(dbConnPoolParams.getValidationQuery());
		ds.setValidationQueryTimeout(dbConnPoolParams.getValidationQueryTimeoutSeconds());
		ds.setMaxConnLifetimeMillis(dbConnPoolParams.getMaxConnLifetimeMillis());
		ds.setConnectionInitSqls(dbConnPoolParams.getConnectionInitSqls());
		ds.setLogExpiredConnections(dbConnPoolParams.isLogExpiredConnections());
		ds.setCacheState(dbConnPoolParams.isCacheState());
		ds.setDefaultTransactionIsolation(dbConnPoolParams.getDefaultTransactionIsolation());
		ds.setFastFailValidation(dbConnPoolParams.isFastFailValidation());
		ds.setDisconnectionSqlCodes(dbConnPoolParams.getDisconnectionSqlCodes());
		ds.setDefaultCatalog(dbConnPoolParams.getDefaultCatalog());
		if(dbConnPoolParams.getConnectionProperties()!=null)
			ds.setConnectionProperties(dbConnPoolParams.getConnectionProperties());
		ds.setAccessToUnderlyingConnectionAllowed(dbConnPoolParams.isAccessToUnderlyingConnectionAllowed());//PoolGuard是否可以获取底层连接
		//Deprecated
//		ds.setEnableAutoCommitOnReturn(autoCommitOnReturn);
		return ds;
	}

/*
	@Bean(name = "dataSourceMaster")
    public DataSource masterDataSource() {
		BasicDataSource db = new BasicDataSource();
		db.setDriverClassName(driverClassName);
		db.setUrl(masterUrl);
		db.setUsername(masterUserName);
		db.setPassword(masterPassword);
		db.setDefaultAutoCommit(defaultAutoCommit);
		db.setMaxIdle(maxIdle);
		db.setMinIdle(minIdle);
		db.setPoolPreparedStatements(poolPreparedStatements);
		db.setMaxOpenPreparedStatements(maxOpenPreparedStatements);
		db.setRemoveAbandonedTimeout(removeAbandonedTimeout);
		db.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
		db.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		db.setTestOnBorrow(testOnBorrow);
		db.setTestWhileIdle(testWhileIdle);
		db.setTestOnReturn(testOnReturn);
		return db;
	}

	@Bean(name = "dataSourceSlave")
    public DataSource slaveDataSource() {
		BasicDataSource db = new BasicDataSource();
		db.setDriverClassName(driverClassName);
		db.setUrl(masterUrl);
		db.setUsername(masterUserName);
		db.setPassword(masterPassword);
		db.setDefaultAutoCommit(defaultAutoCommit);
		db.setMaxIdle(maxIdle);
		db.setMinIdle(minIdle);
		db.setPoolPreparedStatements(poolPreparedStatements);
		db.setMaxOpenPreparedStatements(maxOpenPreparedStatements);
		db.setRemoveAbandonedTimeout(removeAbandonedTimeout);
		db.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
		db.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		db.setTestOnBorrow(testOnBorrow);
		db.setTestWhileIdle(testWhileIdle);
		db.setTestOnReturn(testOnReturn);
		return db;
	}

	@Resource(name = "dataSourceMaster")
	private DataSource masterDB;
	@Resource(name = "dataSourceSlave")
	private DataSource slaveDB;

	@Bean(name = "routingDataSource")
    public DataSource routingDataSource() {
		Map<Object, Object> targetDataSources = new HashMap<Object, Object>(2);
		targetDataSources.put(DataSourceHolder.MASTER, masterDB);
		targetDataSources.put(DataSourceHolder.SLAVE, slaveDB);
		RoutingDataSource  db = new RoutingDataSource();
		db.setTargetDataSources(targetDataSources);
		db.setDefaultTargetDataSource(null);
		return db;
	}

	@Bean
	public PlatformTransactionManager txManager(@Qualifier("routingDataSource")DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Autowired
	private PlatformTransactionManager platformTransactionManager;

	@Override
	public PlatformTransactionManager annotationDrivenTransactionManager() {
		return platformTransactionManager;
	}
*/
	//**************mybatis********************//
/*
	@Value("${mybatis.mapper-locations}")
	private String mybatisMapperLocations;

	@Bean(name="sqlSessionFactory")//name被设置在@MapperScan属性sqlSessionFactoryRef中
	public SqlSessionFactory sessionFactory(@Qualifier("routingDataSource")DataSource dataSource) throws Exception{
		SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
		sessionFactoryBean.setDataSource(dataSource);
		sessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mybatisMapperLocations));
		return sessionFactoryBean.getObject();
	}

	@Bean
	public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory")SqlSessionFactory sqlSessionFactory) throws Exception {
		SqlSessionTemplate template = new SqlSessionTemplate(sqlSessionFactory);
		return template;
	}
*/
	//**************/mybatis********************//
}