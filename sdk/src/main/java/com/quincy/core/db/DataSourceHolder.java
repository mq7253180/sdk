package com.quincy.core.db;

public class DataSourceHolder {
	public final static String MASTER = "master";
	public final static String SLAVE = "slave";
    private static final ThreadLocal<String> dataSources = new ThreadLocal<String>();

    public static void setMaster() {
        dataSources.set(MASTER);
    }
    public static void setSlave() {
        dataSources.set(SLAVE);
    }
    public static String getDetermineCurrentLookupKey() {
        return dataSources.get();
    }
    public static void remove() {
        dataSources.remove();
    }
}
