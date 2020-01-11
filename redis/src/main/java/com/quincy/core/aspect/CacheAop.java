package com.quincy.core.aspect;

import java.lang.reflect.Method;

import javax.annotation.Resource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.sdk.annotation.Cache;
import com.quincy.sdk.helper.CommonHelper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Aspect
@Order(10)
@Component
public class CacheAop {
	@Resource(name = "cacheKeyPrefix")
	private String cacheKeyPrefix;
	@Value("${cache.failover.delaySecs}")
	private int failoverDelaySecs;
	@Value("${cache.failover.retries}")
	private int failoverRetries;
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
		String _key = null;
		if(keyStr.length()>0) {
			_key = cacheKeyPrefix+keyStr;
		} else {
			StringBuilder sb = new StringBuilder(100);
			sb.append(cacheKeyPrefix);
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
	    		_key = sb.toString();
		}
		byte[] key = _key.getBytes();
	    	Jedis jedis = null;
	    	try {
	    		jedis = jedisPool.getResource();
	    		byte[] cache = jedis.get(key);
	    		if(cache==null||cache.length==0) {
	    			byte[] nxKey = (_key+"_nx").getBytes();
	    			long setNx = jedis.setnx(nxKey, nxKey);
	    			if(setNx>0) {
	    				jedis.expire(nxKey, failoverDelaySecs);
	    				Object retVal = joinPoint.proceed();
		        		if(retVal!=null) {
		        			jedis.set(key, CommonHelper.serialize(retVal));
		            		int expire = annotation.expire();
		            		if(expire>0)
		                		jedis.expire(key, expire);
		        		}
		        		return retVal;
	    			} else {
	    				for(int i=0;i<failoverRetries;i++) {
	    					Thread.sleep(failoverDelaySecs*1000);
	    					cache = jedis.get(key);
		    				if(cache!=null&&cache.length>0)
		    					break;
	    				}
	    			}
	    		}
	    		Object toReturn = (cache!=null&&cache.length>0)?CommonHelper.unSerialize(cache):null;
	    		return toReturn;
	    	} finally {
	    		if(jedis!=null)
	    			jedis.close();
	    	}
    }
}