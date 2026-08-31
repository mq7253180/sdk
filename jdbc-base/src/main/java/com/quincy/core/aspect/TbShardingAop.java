package com.quincy.core.aspect;

import javax.annotation.PostConstruct;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

import com.quincy.core.jdbc.TbShardingConnection;

@Order(7)
@Aspect
public class TbShardingAop {
	@Value("${spring.datasource.tdsharding.count}")
	private Integer shardingCount;
	@Value("${spring.datasource.tdsharding.tableNames}")
	private String[] tableNames;

	@Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalPointCut() {}
	@Pointcut("@annotation(com.quincy.sdk.annotation.jdbc.ReadOnly)")
    public void readOnlyPointCut() {}

	private Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		long shardingKey = ShardingKeyUtil.get(joinPoint);
		TbShardingConnection.setShard(ShardingKeyUtil.shard((shardingKey+"a").hashCode(), shardingCount));
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

	@PostConstruct
	public void init() {
		Assert.isTrue(shardingCount!=null&&shardingCount>0, "spring.datasource.tdsharding.count must be specified and greater than 0 when TbSharding is enabled!!!");
		Assert.isTrue(tableNames!=null&&tableNames.length>0, "spring.datasource.tdsharding.tableNames must be specified when TbSharding is enabled!!!");
		TbShardingConnection.setTableNames(tableNames);
	}
}