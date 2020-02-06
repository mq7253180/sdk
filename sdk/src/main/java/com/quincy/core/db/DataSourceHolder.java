package com.quincy.core.db;

public class DataSourceHolder {
	public final static String MASTER = "master";
	public final static String SLAVE = "slave";
    private static final ThreadLocal<String> dataSource = new ThreadLocal<String>();

    public static void setMaster() {
        dataSource.set(MASTER);
    }
    public static void setSlave() {
        dataSource.set(SLAVE);
    }
    public static String getDetermineCurrentLookupKey() {
        return dataSource.get();
    }
    public static void remove() {
        dataSource.remove();
    }
}
