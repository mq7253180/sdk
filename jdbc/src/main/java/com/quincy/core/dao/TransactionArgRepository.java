package com.quincy.core.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.quincy.core.entity.TransactionArg;

@Repository
public interface TransactionArgRepository extends JpaRepository<TransactionArg, Long>, JpaSpecificationExecutor<TransactionArg> {
	public List<TransactionArg> findByParentIdAndTypeOrderBySort(Long parentId, Integer type);
}