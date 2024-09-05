package com.quincy.auth.aspect;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.sdk.AuthHelper;
import com.quincy.sdk.helper.AopHelper;
import com.quincy.sdk.o.XSession;

@Order(10)
@Aspect
@Component
public class SessionInjectorAop {
	@Pointcut("@annotation(com.quincy.sdk.annotation.auth.XSessionInject)")
	public void xSessionInjectPointCut() {}

	@Around("xSessionInjectPointCut()")
    public Object xSessionInjectAround(ProceedingJoinPoint joinPoint) throws Throwable {
		return this.doAround(joinPoint);
	}

	@Pointcut("@annotation(com.quincy.sdk.annotation.auth.PermissionNeeded)")
    public void permissionNeededPointCut() {}

	@Around("permissionNeededPointCut()")
    public Object permissionNeededAround(ProceedingJoinPoint joinPoint) throws Throwable {
		return this.doAround(joinPoint);
	}

	private Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    	Class<?>[] classes = methodSignature.getParameterTypes();
    	Object[] args = joinPoint.getArgs();
    	List<Integer> index = new ArrayList<Integer>(classes.length);
    	for(int i=0;i<classes.length;i++) {
    		String className = classes[i].getName();
    		if(XSession.class.getName().equals(className)&&(args[i]==null||AopHelper.isControllerMethod(joinPoint)))
    			index.add(i);
    	}
    	if(index.size()>0) {
    		XSession session = AuthHelper.getSession();
    		for(Integer i:index)
    			args[i] = session;
    	}
    	return joinPoint.proceed(args);
	}
}