package com.quincy.core.aspect;

import java.lang.reflect.Method;
import java.net.InetAddress;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.core.redis.JedisSource;
import com.quincy.core.redis.RedisConstants;
import com.quincy.sdk.RedisProcessor;
import com.quincy.sdk.annotation.L2Cache;
import com.quincy.sdk.helper.CommonHelper;

import redis.clients.jedis.Jedis;

@Order(3)
@Aspect
@Component
public class SecondaryCacheAop {
	@Value("${spring.redis.key.prefix}")
	private String keyPrefix;
	@Autowired
	@Qualifier(RedisConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
	private JedisSource jedisSource;
	@Autowired
	private RedisProcessor redisProcessor;

	@Pointcut("@annotation(com.quincy.sdk.annotation.L2Cache)")
    public void pointCut() {}

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    	Class<?> clazz = joinPoint.getTarget().getClass();
    	MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    	Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
    	L2Cache annotation = method.getAnnotation(L2Cache.class);
    	String keyStr = annotation.key().trim();
    	String _key = keyPrefix+"cache:"+(keyStr.length()>0?keyStr:CommonHelper.fullMethodPath(clazz, methodSignature, method, joinPoint.getArgs(), ".", "_", "#"));
    	byte[] key = (_key+":VALUE").getBytes();
    	Jedis jedis = null;
    	try {
    		jedis = jedisSource.get();
    		byte[] cache = jedis.get(key);
    		if(cache==null||cache.length==0) {
    			String nxKey = _key+":NX";
    			long setNx = jedis.setnx(nxKey, InetAddress.getLocalHost().getHostAddress()+"-"+Thread.currentThread().threadId());
    			if(setNx>0) {
    				jedis.expire(nxKey, annotation.setnxExpire());
    				Object retVal = this.invokeAndCache(jedis, joinPoint, annotation, key);
    				jedis.del(nxKey);
    				return retVal;
    			} else {
    				for(int i=0;i<annotation.retries();i++) {
    					Thread.sleep(annotation.millisBetweenRetries());
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

    private Object invokeAndCache(Jedis jedis, ProceedingJoinPoint joinPoint, L2Cache annotation, byte[] key) throws Throwable {
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