package com.quincy;

import java.util.List;

import com.quincy.sdk.MasterOrSlave;
import com.quincy.sdk.annotation.AllShardSQL;
import com.quincy.sdk.annotation.Select;

@AllShardSQL
public interface TestMapper {
	@Select(value = "SELECT * FROM s_user;", masterOrSlave = MasterOrSlave.SLAVE)
	public List<?> findAllUsers();
//	@SelectSQL("SELECT * FROM b_order;")
//	public List<?> findAllOrders();
}