package com.quincy.core.db;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.quincy.core.db.DataSourceHolder;

public class RoutingDataSource extends AbstractRoutingDataSource {
	@Override
	protected Object determineCurrentLookupKey() {
		return DataSourceHolder.getDetermineCurrentLookupKey();
	}
}