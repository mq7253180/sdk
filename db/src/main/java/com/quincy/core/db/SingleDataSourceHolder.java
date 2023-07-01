package com.quincy.core.db;

public class SingleDataSourceHolder extends DataSourceHolder {
	public final static String MASTER = "master";
	public final static String SLAVE = "slave";

    public static void setMaster() {
        set(MASTER);
    }
    public static void setSlave() {
        set(SLAVE);
    }
}