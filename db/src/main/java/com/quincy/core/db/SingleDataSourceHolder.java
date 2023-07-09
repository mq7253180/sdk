package com.quincy.core.db;

import com.quincy.sdk.MasterOrSlave;

public class SingleDataSourceHolder extends DataSourceHolder {
    public static void setMaster() {
        set(MasterOrSlave.MASTER.value());
    }

    public static void setSlave() {
        set(MasterOrSlave.SLAVE.value());
    }
}