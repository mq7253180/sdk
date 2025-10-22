package com.quincy.core.dao;

import java.util.List;

import com.quincy.core.entity.TransactionAtomic;
import com.quincy.sdk.annotation.jdbc.ExecuteQuery;
import com.quincy.sdk.annotation.jdbc.ExecuteUpdate;
import com.quincy.sdk.annotation.jdbc.JDBCDao;

@JDBCDao
public interface TransactionAtomicDao {
	@ExecuteUpdate(sql = "INSERT INTO s_transaction_atomic(tx_id, bean_name, method_name, sort) VALUES(? ,? ,? ,?)")
	public int insert(Long txId, String beanName, String methodName, Integer sort);
	@ExecuteUpdate(sql = "UPDATE s_transaction_atomic SET status=?,msg=?,ret_class=?,ret_value=? WHERE id=?")
	public int update(Integer status, String msg, String retClass, String retValue, Long id);
	@ExecuteQuery(sql = "SELECT * FROM s_transaction_atomic WHERE id=?", returnItemType = TransactionAtomic.class)
	public TransactionAtomic find(Long id);
	@ExecuteQuery(sql = "SELECT * FROM s_transaction_atomic WHERE tx_id=? AND status=? ORDER BY sort", returnItemType = TransactionAtomic.class)
	public List<TransactionAtomic> findByTxIdAndStatusOrderBySort(Long txId, Integer status);
	@ExecuteQuery(sql = "SELECT * FROM s_transaction_atomic WHERE tx_id=? AND status=? ORDER BY sort DESC", returnItemType = TransactionAtomic.class)
	public List<TransactionAtomic> findByTxIdAndStatusOrderBySortDesc(Long txId, Integer status);
}