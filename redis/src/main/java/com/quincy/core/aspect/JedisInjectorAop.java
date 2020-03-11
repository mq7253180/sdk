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
import com.quincy.core.redis.QuincyJedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

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
    	Class<?>[] classes = methodSignature.getParameterTypes();
    	Object[] args = joinPoint.getArgs();
    	Jedis jedis = null;
    	JedisCluster jedisCluster = null;
    	try {
    		for(int i=0;i<classes.length;i++) {
    			if(jedis==null)
    				jedis = jedisSource.get();
    			String className = classes[i].getName();
    			if(Jedis.class.getName().equals(className)) {
    				args[i] = jedis;
    			} else if(JedisCluster.class.getName().equals(className)) {
    				if(jedisCluster==null)
    					jedisCluster = ((QuincyJedis)jedis).getJedisCluster();
    				args[i] = jedisCluster;
    			}
    		}
    		return joinPoint.proceed(args);
    	} finally {
    		if(jedisCluster==null&&jedis!=null)
    			jedis.close();
    	}
    }
}