package com.quincy.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.quincy.core.db.DataSourceHolder;
import com.quincy.core.db.RoutingDataSource;
import com.quincy.sdk.Constants;
import com.quincy.sdk.PoolParams;

@PropertySource("classpath:application-sdk.properties")
@Configuration
//@AutoConfigureAfter(CommonApplicationContext.class)
//@Import(CommonApplicationContext.class)
public class CoreApplicationContext {//implements TransactionManagementConfigurer {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;
	@Autowired
	private PoolParams poolParams;

	@Bean(name = "dataSource")
    public DataSource routingDataSource() {
		boolean removeAbandonedOnMaintenance = Boolean.parseBoolean(properties.getProperty("spring.datasource.pool.removeAbandonedOnMaintenance"));
		boolean removeAbandonedOnBorrow = Boolean.parseBoolean(properties.getProperty("spring.datasource.pool.removeAbandonedOnBorrow"));
		int removeAbandonedTimeout = Integer.parseInt(properties.getProperty("spring.datasource.pool.removeAbandonedTimeout"));
		boolean defaultAutoCommit = Boolean.parseBoolean(properties.getProperty("spring.datasource.dbcp2.defaultAutoCommit"));
		boolean poolPreparedStatements = Boolean.parseBoolean(properties.getProperty("spring.datasource.dbcp2.poolPreparedStatements"));
		int maxOpenPreparedStatements = Integer.parseInt(properties.getProperty("spring.datasource.dbcp2.maxOpenPreparedStatements"));
		String driverClassName = properties.getProperty("spring.datasource.driver-class-name");
		String masterUrl = properties.getProperty("spring.datasource.url");
		String masterUserName = properties.getProperty("spring.datasource.username");
		String masterPassword = properties.getProperty("spring.datasource.password");
		String slaveUrl = properties.getProperty("spring.datasource.url.slave");
		String slaveUserName = properties.getProperty("spring.datasource.username.slave");
		String slavePassword = properties.getProperty("spring.datasource.password.slave");

		BasicDataSource masterDB = new BasicDataSource();
		masterDB.setMaxTotal(poolParams.getMaxTotal());
		masterDB.setMaxIdle(poolParams.getMaxIdle());
		masterDB.setMinIdle(poolParams.getMinIdle());
		masterDB.setMaxWaitMillis(poolParams.getMaxWaitMillis());
		masterDB.setMinEvictableIdleTimeMillis(poolParams.getMinEvictableIdleTimeMillis());
		masterDB.setTimeBetweenEvictionRunsMillis(poolParams.getTimeBetweenEvictionRunsMillis());
		masterDB.setNumTestsPerEvictionRun(poolParams.getNumTestsPerEvictionRun());
		masterDB.setTestOnBorrow(poolParams.getTestOnBorrow());
		masterDB.setTestWhileIdle(poolParams.getTestWhileIdle());
		masterDB.setTestOnReturn(poolParams.getTestOnReturn());
		masterDB.setRemoveAbandonedOnMaintenance(removeAbandonedOnMaintenance);
		masterDB.setRemoveAbandonedOnBorrow(removeAbandonedOnBorrow);
		masterDB.setRemoveAbandonedTimeout(removeAbandonedTimeout);

		masterDB.setDriverClassName(driverClassName);
		masterDB.setUrl(masterUrl);
		masterDB.setUsername(masterUserName);
		masterDB.setPassword(masterPassword);

		masterDB.setDefaultAutoCommit(defaultAutoCommit);
		masterDB.setPoolPreparedStatements(poolPreparedStatements);
		masterDB.setMaxOpenPreparedStatements(maxOpenPreparedStatements);

		BasicDataSource slaveDB = new BasicDataSource();
		slaveDB.setMaxTotal(poolParams.getMaxTotal());
		slaveDB.setMaxIdle(poolParams.getMaxIdle());
		slaveDB.setMinIdle(poolParams.getMinIdle());
		slaveDB.setMaxWaitMillis(poolParams.getMaxWaitMillis());
		slaveDB.setMinEvictableIdleTimeMillis(poolParams.getMinEvictableIdleTimeMillis());
		slaveDB.setTimeBetweenEvictionRunsMillis(poolParams.getTimeBetweenEvictionRunsMillis());
		slaveDB.setNumTestsPerEvictionRun(poolParams.getNumTestsPerEvictionRun());
		slaveDB.setTestOnBorrow(poolParams.getTestOnBorrow());
		slaveDB.setTestWhileIdle(poolParams.getTestWhileIdle());
		slaveDB.setTestOnReturn(poolParams.getTestOnReturn());
		slaveDB.setRemoveAbandonedOnMaintenance(removeAbandonedOnMaintenance);
		slaveDB.setRemoveAbandonedOnBorrow(removeAbandonedOnBorrow);
		slaveDB.setRemoveAbandonedTimeout(removeAbandonedTimeout);

		slaveDB.setDriverClassName(driverClassName);
		slaveDB.setUrl(slaveUrl);
		slaveDB.setUsername(slaveUserName);
		slaveDB.setPassword(slavePassword);

		slaveDB.setDefaultAutoCommit(defaultAutoCommit);
		slaveDB.setPoolPreparedStatements(poolPreparedStatements);
		slaveDB.setMaxOpenPreparedStatements(maxOpenPreparedStatements);

		Map<Object, Object> targetDataSources = new HashMap<Object, Object>(2);
		targetDataSources.put(DataSourceHolder.MASTER, masterDB);
		targetDataSources.put(DataSourceHolder.SLAVE, slaveDB);
		RoutingDataSource db = new RoutingDataSource();
		db.setTargetDataSources(targetDataSources);
		db.setDefaultTargetDataSource(masterDB);
		return db;
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
