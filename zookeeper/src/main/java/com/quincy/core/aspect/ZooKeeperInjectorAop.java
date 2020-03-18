package com.quincy.core.aspect;

import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.ZooKeeper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.core.zookeeper.ZooKeeperSource;
import com.quincy.sdk.helper.AopHelper;

@Aspect
@Order(4)
@Component
public class ZooKeeperInjectorAop {
	@Autowired
	private ZooKeeperSource zkSource;

	@Pointcut("@annotation(com.quincy.sdk.annotation.ZooKeeperInjector)")
    public void pointCut() {}

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    		Class<?>[] classes = methodSignature.getParameterTypes();
    		Object[] args = joinPoint.getArgs();
    		List<Integer> index = new ArrayList<Integer>(classes.length);
    		for(int i=0;i<classes.length;i++) {
    			Object arg = args[i];
    			if(ZooKeeper.class.getName().equals(classes[i].getName())&&(arg==null||AopHelper.isControllerMethod(joinPoint)))
    				index.add(i);
    		}
    		if(index.size()>0) {
    			ZooKeeper zk = null;
        		try {
        			zk = zkSource.get();
        			for(int i:index)
        				args[i] = zk;
        			return joinPoint.proceed(args);
        		} finally {
        			if(zk!=null)
        				zk.close();
        		}
    		} else
    			return joinPoint.proceed(args);
    }
}