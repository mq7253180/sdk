package com.quincy.core.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.core.redis.JedisSource;

import redis.clients.jedis.Jedis;

@Aspect
@Order(3)
@Component
public class JedisInjectorAop {
	@Autowired
	private JedisSource jedisSource;

	@Pointcut("@annotation(com.quincy.sdk.annotation.JedisInjector)")
    public void pointCut() {}

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    		Class<?>[] clazzes = methodSignature.getParameterTypes();
    		Object[] args = joinPoint.getArgs();
    		Jedis jedis = null;
    		try {
    			for(int i=0;i<clazzes.length;i++) {
    				Class<?> clazz = clazzes[i];
    				if(Jedis.class.getName().equals(clazz.getName())) {
    					jedis = jedisSource.get();
    					args[i] = jedis;
    					break;
    				}
    			}
    			return joinPoint.proceed(args);
    		} finally {
    			if(jedis!=null)
    				jedis.close();
    		}
    }
}