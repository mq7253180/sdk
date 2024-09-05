package com.quincy.core.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.quincy.core.db.SingleDataSourceHolder;
import com.quincy.sdk.helper.AopHelper;

@Order(6)
@Aspect
@Component
public class ReadOnlyAop {
	@Pointcut("@annotation(com.quincy.sdk.annotation.jdbc.ReadOnly)")
    public void pointCut() {}

	@Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Transactional transactionalAnnotation = AopHelper.getAnnotation(joinPoint, Transactional.class);
		if(transactionalAnnotation==null) {
			boolean stackRoot = false;
			try {
				if(SingleDataSourceHolder.getDetermineCurrentLookupKey()==null) {
					stackRoot = true;
					SingleDataSourceHolder.setSlave();
				}
				return joinPoint.proceed();
			} finally {
				if(stackRoot)
					SingleDataSourceHolder.remove();
			}
		} else {
			return joinPoint.proceed();
		}
	}
}
