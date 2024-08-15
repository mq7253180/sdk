package com.quincy.core.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.quincy.core.entity.TransactionAtomic;

@Repository
public interface TransactionAtomicRepository extends JpaRepository<TransactionAtomic, Long>, JpaSpecificationExecutor<TransactionAtomic> {
	public List<TransactionAtomic> findByTxIdAndStatusOrderBySort(Long txId, Integer status);
	public List<TransactionAtomic> findByTxIdAndStatusOrderBySortDesc(Long txId, Integer status);
}