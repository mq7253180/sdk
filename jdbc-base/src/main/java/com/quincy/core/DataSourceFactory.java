package com.quincy.core;

import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
//@AutoConfigureAfter(CommonApplicationContext.class)
//@Import(CommonApplicationContext.class)
public class DataSourceFactory {//implements TransactionManagementConfigurer {
	@Autowired
	private GenericObjectPoolConfig poolCfg;
	@Autowired
	private AbandonedConfig abandonedCfg;
	@Autowired
	private DBConnPoolParams dbConnPoolParams;

	public BasicDataSource create(int ratio) throws SQLException {
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

	@Autowired
	@Qualifier("dataSourceMaster")
	private DataSource masterDB;
	@Autowired
	@Qualifier("dataSourceSlave")
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