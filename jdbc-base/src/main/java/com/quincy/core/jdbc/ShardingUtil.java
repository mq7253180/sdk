package com.quincy.core.jdbc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.quincy.sdk.SnowFlake;
import com.quincy.sdk.annotation.jdbc.ShardingKey;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class ShardingUtil {
	public static long extractShardingKey(JoinPoint joinPoint) throws NoSuchMethodException, SecurityException {
		Long shardingKey = doExtractShardingKey();
		if(shardingKey!=null) {
			return shardingKey;
		} else {
			Class<?> clazz = joinPoint.getTarget().getClass();
			MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
			Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
			return doExtractShardingKey(method.getParameterAnnotations(), joinPoint.getArgs());
		}
	}

	public static long extractShardingKey(Annotation[][] annotationss, Object[] args) {
		Long shardingKey = doExtractShardingKey();
		return shardingKey==null?null:doExtractShardingKey(annotationss, args);
	}

	private static long doExtractShardingKey(Annotation[][] annotationss, Object[] args) {
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

	public static long shard(long key, int count) {
		return key&(count-1);
	}

	public static long tbShard(long key, int count) {
		return shard((key+"a").hashCode(), count);
	}

	public final static String SESSION_KEY_SHARDING = "SHARDING_KEY";
	public final static String SESSION_KEY_DB_SHARD = "DB_SHARD";
	public final static String SESSION_KEY_TB_SHARD = "TB_SHARD";

	private static Long doExtractShardingKey() {
		return getLongAttrFromSession(SESSION_KEY_SHARDING);
	}

	public static Long getDbShard() {
		return getLongAttrFromSession(SESSION_KEY_DB_SHARD);
	}

	public static Long getTbShard() {
		return getLongAttrFromSession(SESSION_KEY_TB_SHARD);
	}

	private static Long getLongAttrFromSession(String key) {
		Object longObj = getSession().getAttribute(key);
		return longObj==null?null:Long.valueOf(longObj.toString());
	}

	public static void setShardingKey(Long value) {
		getSession().setAttribute(SESSION_KEY_SHARDING, value);
	}

	public static void setDbShard(Long value) {
		getSession().setAttribute(SESSION_KEY_DB_SHARD, value);
	}

	public static void setTbShard(Long value) {
		getSession().setAttribute(SESSION_KEY_TB_SHARD, value);
	}

	private static HttpSession getSession() {
		HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
		return request.getSession();
	}
}
