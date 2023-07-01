package com.quincy.core.db;

public class DataSourceHolder {
    private static final ThreadLocal<String> dataSource = new ThreadLocal<String>();

    public static void set(String key) {
        dataSource.set(key);
    }
    public static String getDetermineCurrentLookupKey() {
        return dataSource.get();
    }
    public static void remove() {
        dataSource.remove();
    }
}
