package com.quincy.core.aspect;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.core.InnerConstants;
import com.quincy.core.redis.JedisSource;
import com.quincy.sdk.RedisProcessor;
import com.quincy.sdk.annotation.Cache;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

@Slf4j
@Aspect
@Order(1)
@Component
public class CacheAop {
	@Autowired
	@Qualifier("cacheKeyPrefix")
	private String cacheKeyPrefix;
	@Autowired
	@Qualifier(InnerConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
	private JedisSource jedisSource;
	@Autowired
	private RedisProcessor redisProcessor;

	@Pointcut("@annotation(com.quincy.sdk.annotation.Cache)")
    public void pointCut() {}

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    	Class<?> clazz = joinPoint.getTarget().getClass();
    	MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    	Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
    	Cache annotation = method.getAnnotation(Cache.class);
    	String keyStr = annotation.key().trim();
    	String _key = cacheKeyPrefix+(keyStr.length()>0?keyStr:CommonHelper.fullMethodPath(clazz, methodSignature, method, joinPoint.getArgs(), ".", "_", "#"));
    	log.info("CACHE_KEY============================={}", _key);
    	byte[] key = (_key+":VALUE").getBytes();
    	Jedis jedis = null;
    	try {
    		jedis = jedisSource.get();
    		byte[] cache = jedis.get(key);
    		if(cache==null||cache.length==0) {
    			byte[] nxKey = (_key+":NX").getBytes();
    			long setNx = jedis.setnx(nxKey, nxKey);
    			if(setNx>0) {
    				jedis.expire(nxKey, annotation.setnxDelaySecs());
    				Object retVal = this.invokeAndCache(jedis, joinPoint, annotation, key);
    				return retVal;
    			} else {
    				for(int i=0;i<annotation.setnxFailRetries();i++) {
    					Thread.sleep(annotation.intervalMillis());
    					cache = jedis.get(key);
    					if(cache!=null&&cache.length>0)
    						break;
    				}
    				if((cache==null||cache.length==0)&&!annotation.returnNull()) {
    					Object retVal = this.invokeAndCache(jedis, joinPoint, annotation, key);
    					return retVal;
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

    private Object invokeAndCache(Jedis jedis, ProceedingJoinPoint joinPoint, Cache annotation, byte[] key) throws Throwable {
    	Object toReturn = joinPoint.proceed();
    	if(toReturn!=null) {
    		byte[] valToCache = CommonHelper.serialize(toReturn);
    		int expire = annotation.expire();
    		if(expire>0) {
    			redisProcessor.setAndExpire(key, valToCache, expire, jedis);
    		} else
    			jedis.set(key, valToCache);
    	}
    	return toReturn;
    }
}