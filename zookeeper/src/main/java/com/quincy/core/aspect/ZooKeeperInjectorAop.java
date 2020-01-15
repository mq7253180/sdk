package com.quincy.core.aspect;

import org.apache.zookeeper.ZooKeeper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.core.zookeeper.ZooKeeperFactory;

@Aspect
@Order(4)
@Component
public class ZooKeeperInjectorAop {
	@Autowired
	private ZooKeeperFactory factory;

	@Pointcut("@annotation(com.quincy.sdk.annotation.ZooKeeperInjector)")
    public void pointCut() {}

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    		Class<?>[] clazzes = methodSignature.getParameterTypes();
    		Object[] args = joinPoint.getArgs();
    		ZooKeeper zk = null;
    		try {
    			for(int i=0;i<clazzes.length;i++) {
    				Class<?> clazz = clazzes[i];
    				if(ZooKeeper.class.getName().equals(clazz.getName())) {
    					zk = factory.connect();
    					args[i] = zk;
    					break;
    				}
    			}
    			return joinPoint.proceed(args);
    		} finally {
    			if(zk!=null)
    				zk.close();
    		}
    }
}