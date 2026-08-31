package com.quincy.core.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.core.jdbc.TbShardingConnection;

@Order(7)
@Aspect
@Component
public class TbShardingAop {
	@Value("${spring.datasource.tdsharding.count}")
	private int shardingCount;

	@Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalPointCut() {}
	@Pointcut("@annotation(com.quincy.sdk.annotation.jdbc.ReadOnly)")
    public void readOnlyPointCut() {}

	private Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		TbShardingConnection.setShard(ShardingKeyHolder.reShard(ShardingKeyHolder.get(joinPoint), shardingCount));
		return joinPoint.proceed();
	}

	@Around("transactionalPointCut()")
    public Object doTransactionalAround(ProceedingJoinPoint joinPoint) throws Throwable {
		return this.doAround(joinPoint);
	}

	@Around("readOnlyPointCut()")
    public Object doReadOnlyAround(ProceedingJoinPoint joinPoint) throws Throwable {
		return this.doAround(joinPoint);
	}
}