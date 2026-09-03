package com.quincy.core.db;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {
	@Override
	protected Object determineCurrentLookupKey() {
		return SingleDataSourceHolder.getDetermineCurrentLookupKey();
	}
}
