package com.quincy.core.dao;

import java.util.Date;
import java.util.List;

import com.quincy.core.entity.Transaction;
import com.quincy.sdk.annotation.jdbc.ExecuteQuery;
import com.quincy.sdk.annotation.jdbc.ExecuteUpdate;
import com.quincy.sdk.annotation.jdbc.JDBCDao;

@JDBCDao
public interface TransactionDao {
	@ExecuteUpdate(sql = "INSERT INTO s_transaction(application_name, bean_name, method_name, type, flag_for_cron_job) VALUES(?, ?, ?, ?, ?)")
	public int insert(String applicationName, String beanName, String methodName, Integer type, String flagForCronJob);
	@ExecuteUpdate(sql = "UPDATE s_transaction SET status=?,last_executed=? WHERE id=?")
	public int update(Integer status, Date lastExecuted, Long id);
	@ExecuteQuery(sql = "SELECT * FROM s_transaction WHERE id=?", returnItemType = Transaction.class)
	public Transaction find(Long id);
	@ExecuteQuery(sql = "SELECT * FROM s_transaction WHERE application_name=? AND status=?", returnItemType = Transaction.class)
	public List<Transaction> findByApplicationNameAndStatus(String applicationName, int status);
	@ExecuteQuery(sql = "SELECT * FROM s_transaction WHERE application_name=? AND status=? AND flag_for_cron_job=?", returnItemType = Transaction.class)
	public List<Transaction> findByApplicationNameAndStatusAndFlagForCronJob(String applicationName, int status, String flagForCronJob);
}