package com.quincy.core.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CoreMapper {
	public int deleteTransactionAtomicArgs(@Param("txId")Long txId);
	public int deleteTransactionAtomics(@Param("txId")Long txId);
	public int deleteArgs(@Param("parentId")Long parentId, @Param("type")Integer type);
	public int deleteTransaction(@Param("id")Long id);
	public int updateTransactionVersion(@Param("id")Long id, @Param("version")Integer version);
	public int updateTransactionAtomicArgs(@Param("txId")Long txId, @Param("classFrom")String classFrom, @Param("classTo")String classTo, @Param("_value")String value);
}