package com.quincy.core.service;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.quincy.core.entity.Transaction;
import com.quincy.core.entity.TransactionArg;
import com.quincy.core.entity.TransactionAtomic;

public interface TransactionService {
	public Transaction insertTransaction(Transaction tx) throws JsonProcessingException;
	public Transaction updateTransaction(Transaction _tx);
	public TransactionAtomic updateTransactionAtomic(TransactionAtomic _atomic);
	public void deleteTransaction(Long id);
	public List<Transaction> findFailedTransactions(String applicationName, String frequencyBatch);
	public int updateTransactionVersion(Long id, Integer version);
	public List<TransactionAtomic> findTransactionAtomics(Long txId, Integer status) throws ClassNotFoundException, IOException;
	public List<TransactionArg> findArgs(Long parentId, Integer type);
}