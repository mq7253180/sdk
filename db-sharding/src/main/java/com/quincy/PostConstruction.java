package com.quincy;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class PostConstruction {
	@Autowired
	private AllShardingConfiguration allShardingConfiguration;
	@Autowired
	private DataSource dataSource;

	@PostConstruct
	public void init() {
		allShardingConfiguration.setDataSource(dataSource);
	}
}