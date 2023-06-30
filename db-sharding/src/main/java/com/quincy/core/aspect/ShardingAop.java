package com.quincy.core.aspect;

import javax.transaction.Transactional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.sdk.helper.AopHelper;

@Aspect
@Order(6)
@Component
public class ShardingAop {
	@Pointcut("@annotation(com.quincy.sdk.annotation.Shard)")
    public void pointCut() {}

	@Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Transactional transactionalAnnotation = AopHelper.getAnnotation(joinPoint, Transactional.class);
		if(transactionalAnnotation==null) {
			boolean stackRoot = false;
			try {
//				if(DataSourceHolder.getDetermineCurrentLookupKey()==null) {
//					stackRoot = true;
//					DataSourceHolder.setSlave();
//				}
				return joinPoint.proceed();
			} finally {
//				if(stackRoot)
//					DataSourceHolder.remove();
			}
		} else {
			return joinPoint.proceed();
		}
	}
}
