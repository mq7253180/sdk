package com.quincy.core.db;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {
	private static final ThreadLocal<String> LOOKUP_KEY = new ThreadLocal<String>();

    public static void setLookupKey(String key) {
    	LOOKUP_KEY.set(key);
    }
    public static String getLookupKey() {
        return LOOKUP_KEY.get();
    }
    public static void removeLookupKey() {
    	LOOKUP_KEY.remove();
    }

	@Override
	protected Object determineCurrentLookupKey() {
		return LOOKUP_KEY.get();
	}
}
