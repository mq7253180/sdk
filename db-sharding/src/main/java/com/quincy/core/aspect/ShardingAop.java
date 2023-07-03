package com.quincy.core.aspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.quincy.ShardingBDConstants;
import com.quincy.core.db.DataSourceHolder;
import com.quincy.sdk.annotation.ShardingKey;

@Aspect
@Order(6)
@Component
public class ShardingAop {
	@Value("${spring.datasource.sharding.count}")
	private int shardingCount;

	@Pointcut("@annotation(com.quincy.sdk.annotation.Sharding)")
    public void shardingPointCut() {}
	@Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalPointCut() {}
	@Pointcut("@annotation(com.quincy.sdk.annotation.ReadOnly)")
    public void readOnlyPointCut() {}

	private Object doAround(ProceedingJoinPoint joinPoint, String masterOrSlave) throws Throwable {
		boolean stackRoot = false;
		try {
			if(DataSourceHolder.getDetermineCurrentLookupKey()==null) {
				stackRoot = true;
				Class<?> clazz = joinPoint.getTarget().getClass();
				MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
	    		Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
	    		Annotation[][] annotationss = method.getParameterAnnotations();
	    		int index = -1;
	    		for(int i=0;i<annotationss.length;i++) {
	    			Annotation[] annotations = annotationss[i];
	    			for(int j=0;j<annotations.length;j++) {
	    				if(annotations[j] instanceof ShardingKey) {
	    					index = i;
			    			break;
			    		}
	    			}
	    		}
	    		Assert.isTrue(index>-1, "Sharding key must be specified using @ShardingKey before parameter with type of Long!!!");
	    		Object[] args = joinPoint.getArgs();
		    	Object argObj = args[index];
		    	Assert.isInstanceOf(Long.class, argObj, "Parameter of sharding key must be Long!!!");
		    	Long shardingArg = Long.valueOf(argObj.toString());
		    	long ramainder = shardingArg%shardingCount;
		    	DataSourceHolder.set(masterOrSlave+ramainder);
			}
			return joinPoint.proceed();
		} finally {
			if(stackRoot)
				DataSourceHolder.remove();
		}
	}

	@Around("shardingPointCut()")
    public Object doShardingAround(ProceedingJoinPoint joinPoint) throws Throwable {
		return this.doAround(joinPoint, ShardingBDConstants.MASTER);
	}

	@Around("transactionalPointCut()")
    public Object doTransactionalAround(ProceedingJoinPoint joinPoint) throws Throwable {
		return this.doAround(joinPoint, ShardingBDConstants.MASTER);
	}

	@Around("readOnlyPointCut()")
    public Object doReadOnlyAround(ProceedingJoinPoint joinPoint) throws Throwable {
		return this.doAround(joinPoint, ShardingBDConstants.SLAVE);
	}
}