package com.quincy.core.aspect;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.sdk.Constants;
import com.quincy.sdk.annotation.Cache;
import com.quincy.sdk.helper.CommonHelper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Aspect
@Order(10)
@Component
public class CacheAop {
	private final static String PREFIX = Constants.GLOBAL_CACHE_KEY_PREFIX+".cache.";
	@Autowired
	private JedisPool jedisPool;

	@Pointcut("@annotation(com.quincy.sdk.annotation.Cache)")
    public void pointCut() {}

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    	MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		Class<?> clazz = joinPoint.getTarget().getClass();
		Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
		Cache annotation = method.getAnnotation(Cache.class);
		String keyStr = annotation.key().trim();
		byte[] key = null;
		if(keyStr.length()>0) {
			key = (PREFIX+keyStr).getBytes();
		} else {
			StringBuilder sb = new StringBuilder(100);
			sb.append(PREFIX);
	    		sb.append(clazz.getName());
	    		sb.append(".");
	    		sb.append(methodSignature.getName());
	    		Class<?>[] clazzes = method.getParameterTypes();
	    		Object[] args = joinPoint.getArgs();
	    		if(args!=null&&args.length>0) {
	    			for(int i=0;i<args.length;i++) {
	    				Object arg = args[i];
	        			sb.append("_");
	        			sb.append(clazzes[i].getName());
	        			sb.append("#");
	        			sb.append(arg==null?"null":arg.toString().trim());
	        		}
	    		}
	    		key = sb.toString().getBytes();
		}
	    	Jedis jedis = null;
	    	try {
	    		jedis = jedisPool.getResource();
	    		byte[] cache = jedis.get(key);
	    		if(cache==null||cache.length==0) {
	        		Object retVal = joinPoint.proceed();
	        		if(retVal!=null) {
	        			jedis.set(key, CommonHelper.serialize(retVal));
	            		int expire = annotation.expire();
	            		if(expire>0) {
	                		jedis.expire(key, expire);
	            		}
	        		}
	        		return retVal;
	    		} else {
	                return CommonHelper.unSerialize(cache);
	    		}
	    	} finally {
	    		if(jedis!=null)
	    			jedis.close();
	    	}
    }
}
