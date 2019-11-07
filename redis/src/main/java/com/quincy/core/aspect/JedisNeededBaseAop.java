package com.quincy.core.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public abstract class JedisNeededBaseAop {
	protected abstract void pointCut();
	protected abstract void destroy(JoinPoint joinPoint, Jedis jedis, Object obj);
	protected abstract Object handle(JoinPoint joinPoint, Jedis jedis) throws Throwable;

	@Autowired
	private JedisPool jedisPool;

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    	Jedis jedis = null;
    	try {
    		jedis = jedisPool.getResource();
    		Object obj = this.handle(joinPoint, jedis);
    		Object result = joinPoint.proceed();
            this.destroy(joinPoint, jedis, obj);
            return result;
    	} finally {
    		if(jedis!=null) {
    			jedis.close();
    		}
    	}
    }
}
