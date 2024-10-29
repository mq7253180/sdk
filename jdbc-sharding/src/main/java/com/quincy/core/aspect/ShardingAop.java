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

import com.quincy.core.db.DataSourceHolder;
import com.quincy.sdk.MasterOrSlave;
import com.quincy.sdk.SnowFlake;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.annotation.sharding.Sharding;
import com.quincy.sdk.annotation.sharding.ShardingKey;

@Order(6)
@Aspect
@Component
public class ShardingAop {
	@Value("${spring.datasource.sharding.count}")
	private int shardingCount;

	@Pointcut("@annotation(com.quincy.sdk.annotation.sharding.Sharding)")
    public void shardingPointCut() {}
	@Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalPointCut() {}
	@Pointcut("@annotation(com.quincy.sdk.annotation.jdbc.ReadOnly)")
    public void readOnlyPointCut() {}

	private Object doAround(ProceedingJoinPoint joinPoint, String masterOrSlave) throws Throwable {
		boolean stackRoot = false;
		try {
			Class<?> clazz = joinPoint.getTarget().getClass();
			MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    		Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
    		ReadOnly readOnly = method.getDeclaredAnnotation(ReadOnly.class);
    		Sharding sharding = method.getAnnotation(Sharding.class);
    		boolean reRoute = (readOnly!=null&&readOnly.reRoute())||(sharding!=null&&sharding.reRoute());
			if(reRoute||DataSourceHolder.getDetermineCurrentLookupKey()==null) {
				stackRoot = true;
	    		Annotation[][] annotationss = method.getParameterAnnotations();
	    		int index = -1;
	    		boolean snowFlake = false;
	    		for(int i=0;i<annotationss.length;i++) {
	    			Annotation[] annotations = annotationss[i];
	    			boolean stopLoop = false;
	    			for(int j=0;j<annotations.length;j++) {
	    				if(annotations[j] instanceof ShardingKey) {
	    					index = i;
	    					ShardingKey annotation = (ShardingKey)annotations[j];
	    					snowFlake = annotation.snowFlake();
	    					stopLoop = true;
			    			break;
			    		}
	    			}
	    			if(stopLoop)
	    				break;
	    		}
	    		Assert.isTrue(index>-1, "Sharding key must be specified using @ShardingKey before parameter, and with type of Integer or Long!!!");
	    		Object[] args = joinPoint.getArgs();
		    	Object shardingArgObj = args[index];
		    	Assert.isTrue(shardingArgObj instanceof Integer||shardingArgObj instanceof Long, "Only Long or Integer are acceptable as parameter of sharding key!!!");
		    	Long shardingArg = Long.valueOf(shardingArgObj.toString());
		    	Long shardingKey = snowFlake?SnowFlake.extractShardingKey(shardingArg):shardingArg;
		    	if(shardingKey<0)
		    		shardingKey = shardingKey*-1;
		    	Long ramainder = shardingKey&(shardingCount-1);
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
		return this.doAround(joinPoint, MasterOrSlave.MASTER.value());
	}

	@Around("transactionalPointCut()")
    public Object doTransactionalAround(ProceedingJoinPoint joinPoint) throws Throwable {
		return this.doAround(joinPoint, MasterOrSlave.MASTER.value());
	}

	@Around("readOnlyPointCut()")
    public Object doReadOnlyAround(ProceedingJoinPoint joinPoint) throws Throwable {
		return this.doAround(joinPoint, MasterOrSlave.SLAVE.value());
	}

	public static void main(String[] args) {
    	long l2 = 2l;
    	Object o = l2;
    	System.out.println(o.getClass().getName().equals(long.class.getName()));
    	System.out.println(o.getClass().getName().equals(Long.class.getName()));
    	System.out.println(o instanceof Long);
    	System.out.println(Long.class.isAssignableFrom(o.getClass()));
    	System.out.println(long.class.isAssignableFrom(o.getClass()));
    	System.out.println(o.getClass().isAssignableFrom(Long.class));
    	System.out.println(o.getClass().isAssignableFrom(long.class));
	}
}