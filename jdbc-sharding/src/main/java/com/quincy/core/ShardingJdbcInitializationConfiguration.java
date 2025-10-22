package com.quincy.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.db.RoutingDataSource;

@Configuration(ShardingJdbcHolder.INIT_CONFIGURATION_BEAN_NAME)
public class ShardingJdbcInitializationConfiguration {
	@Autowired
	private DataSource dataSource;
	@Autowired
	private ShardingJdbcDaoConfiguration shardingJdbcDaoConfiguration;
	@Autowired
	private JdbcInitializationConfiguration jdbcPostConstruction;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		RoutingDataSource rds = (RoutingDataSource)dataSource;
		int shardCount = rds.getResolvedDataSources().size()/2;
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(shardCount);
		shardingJdbcDaoConfiguration.setClassMethodMap(jdbcPostConstruction.getClassMethodMap());
		shardingJdbcDaoConfiguration.setDataSource(rds);
		shardingJdbcDaoConfiguration.setThreadPoolExecutor(new ThreadPoolExecutor(shardCount, shardCount, 5, TimeUnit.SECONDS, workQueue, new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				throw new RejectedExecutionException("分片执行繁忙，稍后再试！");
			}
		}));
	}
}