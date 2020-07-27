package com.quincy.core.aspect;

import javax.annotation.Resource;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;

import com.quincy.core.InnerConstants;
import com.quincy.core.redis.JedisSource;

import redis.clients.jedis.Jedis;

public abstract class JedisNeededBaseAop {
	protected abstract void pointCut();
	protected abstract Object before(JoinPoint joinPoint, Jedis jedis) throws Throwable;
	protected abstract void after(JoinPoint joinPoint, Jedis jedis, Object obj);

	@Resource(name = InnerConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
	private JedisSource jedisSource;

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    	Jedis jedis = null;
    	try {
    		jedis = jedisSource.get();
    		Object obj = this.before(joinPoint, jedis);
    		Object result = joinPoint.proceed();
    		this.after(joinPoint, jedis, obj);
    		return result;
    	} finally {
    		if(jedis!=null)
    			jedis.close();
    	}
    }
}