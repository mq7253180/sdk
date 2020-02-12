package com.quincy.core.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public abstract class JedisNeededBaseAop {
	protected abstract void pointCut();
	protected abstract Object before(JoinPoint joinPoint, Jedis jedis) throws Throwable;
	protected abstract void after(JoinPoint joinPoint, Jedis jedis, Object obj);

	@Autowired
	private Pool<Jedis> jedisPool;

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    		Jedis jedis = null;
    		try {
    			jedis = jedisPool.getResource();
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