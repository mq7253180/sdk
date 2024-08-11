package com.quincy.core.aspect;

import java.lang.annotation.Annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.quincy.core.redis.JedisSource;
import com.quincy.core.redis.RedisConstants;
import com.quincy.sdk.helper.AopHelper;

import redis.clients.jedis.Jedis;

public abstract class JedisNeededBaseAop<T extends Annotation> {
	protected abstract void pointCut();
	protected abstract Class<T> annotationType();
	protected abstract Object before(Jedis jedis, T annotation) throws Throwable;
	protected abstract void after(Jedis jedis, Object passFromBefore);

	@Autowired
	@Qualifier(RedisConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
	private JedisSource jedisSource;

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    	Jedis jedis = null;
    	try {
    		jedis = jedisSource.get();
    		T annotation = AopHelper.getAnnotation(joinPoint, this.annotationType());
    		Object passToAfter = this.before(jedis, annotation);
    		Object result = joinPoint.proceed();
    		this.after(jedis, passToAfter);
    		return result;
    	} finally {
    		if(jedis!=null)
    			jedis.close();
    	}
    }
}