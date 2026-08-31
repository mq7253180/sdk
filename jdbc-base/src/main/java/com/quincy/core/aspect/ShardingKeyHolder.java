package com.quincy.core.aspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.Assert;

import com.quincy.sdk.SnowFlake;
import com.quincy.sdk.annotation.jdbc.ShardingKey;

public class ShardingKeyHolder {
	public static long get(JoinPoint joinPoint) throws NoSuchMethodException, SecurityException {
		Class<?> clazz = joinPoint.getTarget().getClass();
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
		return get(method.getParameterAnnotations(), joinPoint.getArgs());
	}

	public static long get(Annotation[][] annotationss, Object[] args) {
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
    	Object shardingArgObj = args[index];
    	Assert.notNull(shardingArgObj, "The value of @ShardingKey specified can not be null.");
    	Assert.isTrue(shardingArgObj instanceof Integer||shardingArgObj instanceof Long, "Only Long or Integer are acceptable as parameter of sharding key!!!");
    	Long shardingArg = Long.valueOf(shardingArgObj.toString());
    	Long shardingKey = snowFlake?SnowFlake.extractShardingKey(shardingArg):shardingArg;
    	if(shardingKey<0)
    		shardingKey = shardingKey*-1;
    	return shardingKey;
	}

	public static long reShard(long key, int count) {
		return (key+"a").hashCode()%count;
	}
}
