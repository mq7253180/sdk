package com.quincy.core.jdbc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.Assert;

import com.quincy.sdk.SnowFlake;
import com.quincy.sdk.annotation.jdbc.ShardingKey;
import com.quincy.sdk.helper.CommonHelper;

public class ShardingUtil {
	public static Long extractShardingKey(JoinPoint joinPoint) throws NoSuchMethodException, SecurityException {
		Class<?> clazz = joinPoint.getTarget().getClass();
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
		return extractShardingKey(method.getParameterAnnotations(), joinPoint.getArgs());
	}

	public static Long extractShardingKey(Annotation[][] annotationss, Object[] args) {
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
//		Assert.isTrue(index>-1, "Sharding key must be specified using @ShardingKey before parameter, and with type of Integer or Long!!!");
    	if(index==-1)
    		return null;
		Object shardingArgObj = args[index];
    	Assert.notNull(shardingArgObj, "The value qualified by @ShardingKey can not be null.");
    	Assert.isTrue(shardingArgObj instanceof Integer||shardingArgObj instanceof Long, "Only Long or Integer are acceptable as parameter of sharding key!!!");
    	Long shardingArg = Long.valueOf(shardingArgObj.toString());
    	Long shardingKey = snowFlake?SnowFlake.extractShardingKey(shardingArg):shardingArg;
    	if(shardingKey<0)
    		shardingKey = shardingKey*-1;
    	return shardingKey;
	}

	public static long shard(long key, int count) {
		return key&(count-1);
	}

	public static long tbShard(long key, int count) {
		return shard((key+"a").hashCode(), count);
	}

	private final static String SESSION_KEY_DB_SHARD = "DB_SHARD";
	private final static String SESSION_KEY_TB_SHARD = "TB_SHARD";

	public static Long getDbShard() {
		return getLongAttrFromSession(SESSION_KEY_DB_SHARD);
	}

	public static Long getTbShard() {
		return getLongAttrFromSession(SESSION_KEY_TB_SHARD);
	}

	private static Long getLongAttrFromSession(String key) {
		Object longObj = CommonHelper.getSession().getAttribute(key);
		return longObj==null?null:Long.valueOf(longObj.toString());
	}

	public static void setDbShard(long key, int count) {
		CommonHelper.getSession().setAttribute(SESSION_KEY_DB_SHARD, shard(key, count));
	}

	public static void setTbShard(long key, int count) {
		CommonHelper.getSession().setAttribute(SESSION_KEY_TB_SHARD, tbShard(key, count));
	}
}
