package com.quincy.core.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.quincy.core.entity.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
	public List<Transaction> findByApplicationNameAndStatus(String applicationName, int status);
	public List<Transaction> findByApplicationNameAndStatusAndFlagForCronJob(String applicationName, int status, String flagForCronJob);
}
