package com.quincy.core.dao;

import java.util.List;

import com.quincy.core.entity.TransactionArg;
import com.quincy.sdk.annotation.jdbc.ExecuteQuery;
import com.quincy.sdk.annotation.jdbc.ExecuteUpdate;
import com.quincy.sdk.annotation.jdbc.JDBCDao;

@JDBCDao
public interface TransactionArgDao {
	@ExecuteUpdate(sql = "INSERT INTO s_transaction_arg(parent_id, class, _value, sort) VALUES(? ,? ,? ,?)")
	public int insert(Long parentId, String clazz, String value, Integer sort);
	@ExecuteQuery(sql = "SELECT * FROM s_transaction_arg WHERE parent_id=? AND type=? ORDER BY sort", returnItemType = TransactionArg.class)
	public List<TransactionArg> findByParentIdAndTypeOrderBySort(Long parentId, Integer type);
}