package com.quincy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.DBCommonPostConstruction;
import com.quincy.core.db.RoutingDataSource;

import jakarta.annotation.PostConstruct;

@Configuration
public class ShardingDBPostConstruction {
	@Autowired
	private DataSource dataSource;
	@Autowired
	private AllShardingConfiguration allShardingConfiguration;
	@Autowired
	private DBCommonPostConstruction dbCommonPostConstruction;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		RoutingDataSource rds = (RoutingDataSource)dataSource;
		int shardCount = rds.getResolvedDataSources().size()/2;
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(shardCount);
		allShardingConfiguration.setClassMethodMap(dbCommonPostConstruction.getClassMethodMap());
		allShardingConfiguration.setDataSource(rds);
		allShardingConfiguration.setThreadPoolExecutor(new ThreadPoolExecutor(shardCount, shardCount, 5, TimeUnit.SECONDS, workQueue, new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				
			}
		}));
	}
}