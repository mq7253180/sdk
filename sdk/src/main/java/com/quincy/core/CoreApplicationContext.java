package com.quincy.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.quincy.core.db.DataSourceHolder;
import com.quincy.core.db.RoutingDataSource;
import com.quincy.sdk.Constants;

@PropertySource("classpath:application-sdk.properties")
@Configuration
//@Order(2)
//@AutoConfigureAfter(CommonApplicationContext.class)
//@Import(CommonApplicationContext.class)
public class CoreApplicationContext {//implements TransactionManagementConfigurer {
	@Resource(name = Constants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@Bean(name = "dataSource")
    public DataSource routingDataSource() {
		String driverClassName = properties.getProperty("spring.datasource.driver-class-name");
		String masterUrl = properties.getProperty("spring.datasource.url");
		String masterUserName = properties.getProperty("spring.datasource.username");
		String masterPassword = properties.getProperty("spring.datasource.password");
		String slaveUrl = properties.getProperty("spring.datasource.url.slave");
		String slaveUserName = properties.getProperty("spring.datasource.username.slave");
		String slavePassword = properties.getProperty("spring.datasource.password.slave");
		boolean defaultAutoCommit = Boolean.parseBoolean(properties.getProperty("spring.datasource.dbcp2.defaultAutoCommit"));
		int maxIdle = Integer.parseInt(properties.getProperty("spring.datasource.dbcp2.maxIdle"));
		int minIdle = Integer.parseInt(properties.getProperty("spring.datasource.dbcp2.minIdle"));
		boolean poolPreparedStatements = Boolean.parseBoolean(properties.getProperty("spring.datasource.dbcp2.poolPreparedStatements"));
		int maxOpenPreparedStatements = Integer.parseInt(properties.getProperty("spring.datasource.dbcp2.maxOpenPreparedStatements"));
		int removeAbandonedTimeout = Integer.parseInt(properties.getProperty("spring.datasource.dbcp2.removeAbandonedTimeout"));
		long timeBetweenEvictionRunsMillis = Long.parseLong(properties.getProperty("spring.datasource.dbcp2.timeBetweenEvictionRunsMillis"));
		long minEvictableIdleTimeMillis = Long.parseLong(properties.getProperty("spring.datasource.dbcp2.minEvictableIdleTimeMillis"));
		boolean testOnBorrow = Boolean.parseBoolean(properties.getProperty("spring.datasource.dbcp2.testOnBorrow"));
		boolean testWhileIdle = Boolean.parseBoolean(properties.getProperty("spring.datasource.dbcp2.testWhileIdle"));
		boolean testOnReturn = Boolean.parseBoolean(properties.getProperty("spring.datasource.dbcp2.testOnReturn"));

		BasicDataSource masterDB = new BasicDataSource();
		masterDB.setDriverClassName(driverClassName);
		masterDB.setUrl(masterUrl);
		masterDB.setUsername(masterUserName);
		masterDB.setPassword(masterPassword);
		masterDB.setDefaultAutoCommit(defaultAutoCommit);
		masterDB.setMaxIdle(maxIdle);
		masterDB.setMinIdle(minIdle);
		masterDB.setPoolPreparedStatements(poolPreparedStatements);
		masterDB.setMaxOpenPreparedStatements(maxOpenPreparedStatements);
		masterDB.setRemoveAbandonedTimeout(removeAbandonedTimeout);
		masterDB.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
		masterDB.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		masterDB.setTestOnBorrow(testOnBorrow);
		masterDB.setTestWhileIdle(testWhileIdle);
		masterDB.setTestOnReturn(testOnReturn);

		BasicDataSource slaveDB = new BasicDataSource();
		slaveDB.setDriverClassName(driverClassName);
		slaveDB.setUrl(slaveUrl);
		slaveDB.setUsername(slaveUserName);
		slaveDB.setPassword(slavePassword);
		slaveDB.setDefaultAutoCommit(defaultAutoCommit);
		slaveDB.setMaxIdle(maxIdle);
		slaveDB.setMinIdle(minIdle);
		slaveDB.setPoolPreparedStatements(poolPreparedStatements);
		slaveDB.setMaxOpenPreparedStatements(maxOpenPreparedStatements);
		slaveDB.setRemoveAbandonedTimeout(removeAbandonedTimeout);
		slaveDB.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
		slaveDB.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		slaveDB.setTestOnBorrow(testOnBorrow);
		slaveDB.setTestWhileIdle(testWhileIdle);
		slaveDB.setTestOnReturn(testOnReturn);

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
