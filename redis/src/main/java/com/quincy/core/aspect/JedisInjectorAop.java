package com.quincy.core.aspect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
import com.quincy.sdk.annotation.JedisInjector;
import com.quincy.sdk.helper.AopHelper;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Transaction;

@Slf4j
@Aspect
@Order(3)
@Component
public class JedisInjectorAop {
	private final static String MSG_TX_NOT_SUPPORTED = "Redis transaction can not be supported in cluster mode.";
	@Autowired
	private JedisSource jedisSource;

	@Pointcut("@annotation(com.quincy.sdk.annotation.JedisInjector)")
    public void pointCut() {}

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    	MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    	Class<?>[] classes = methodSignature.getParameterTypes();
    	Object[] args = joinPoint.getArgs();
    	List<Integer> index = new ArrayList<Integer>(classes.length);
    	for(int i=0;i<classes.length;i++) {
    		String className = classes[i].getName();
    		if((Jedis.class.getName().equals(className)||Transaction.class.getName().equals(className)||JedisCluster.class.getName().equals(className))&&(args[i]==null||AopHelper.isControllerMethod(joinPoint)))
    			index.add(i);
    	}
    	if(index.size()>0) {
    		Class<?> clazz = joinPoint.getTarget().getClass();
    		Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
    		JedisInjector annotation = method.getAnnotation(JedisInjector.class);
    		Jedis jedis = null;
        	JedisCluster jedisCluster = null;
        	Transaction tx = null;
        	boolean commit = true;
        	try {
        		jedis = jedisSource.get();
        		jedisCluster = (jedis instanceof QuincyJedis)?((QuincyJedis)jedis).getJedisCluster():null;
        		if(annotation.transactional()) {
        			if(jedisCluster==null) {
            			tx = jedis.multi();
        			} else
        				throw new RuntimeException(MSG_TX_NOT_SUPPORTED);
        		}
        		for(Integer i:index) {
        			String className = classes[i].getName();
        			if(Jedis.class.getName().equals(className)) {
        				args[i] = jedis;
        			} else if(Transaction.class.getName().equals(className)) {
        				if(tx==null) {
        					throw new RuntimeException(jedisCluster==null?"Redis transation is currently not enabled. Please set 'transactional' to true if you are using redis in transaction.":MSG_TX_NOT_SUPPORTED+" Please remove the argument(s) of Transaction.");
        				} else
        					args[i] = tx;
        			} else if(JedisCluster.class.getName().equals(className)) {
        				if(jedisCluster==null) {
        					throw new RuntimeException("Redis is currently not in cluster mode.");
        				} else
        					args[i] = jedisCluster;
        			} 
        		}
        		return joinPoint.proceed(args);
        	} catch(Throwable e) {
        		if(tx!=null) {
        			Class<? extends Throwable>[] rollbackForClasses = annotation.rollbackFor();
        			if(rollbackForClasses!=null&&rollbackForClasses.length>0) {
        				for(Class<? extends Throwable> rollbackForClazz:rollbackForClasses) {
            				if(rollbackForClazz.getName().equals(e.getClass().getName())) {
            					commit = false;
            					break;
            				}
            			}
        			} else
        				commit = false;
        		}
        		throw e;
        	} finally {
        		if(jedisCluster==null&&jedis!=null) {
        			if(tx!=null) {
        				if(commit) {
        					tx.exec();
        				} else
        					log.info("REDIS_TX_DISCARD============{}", tx.discard());
        				tx.close();
        			}
        			jedis.unwatch();
        			jedis.close();
        		}
        	}
    	} else
    		return joinPoint.proceed(args);
    }
}