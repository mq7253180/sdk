package com.quincy.core.aspect;

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

import com.jcraft.jsch.ChannelSftp;
import com.quincy.core.sftp.ChannelSftpSource;
import com.quincy.sdk.helper.AopHelper;

@Aspect
@Order(5)
@Component
public class ChannelSftpInjectorAop {
	@Autowired
	private ChannelSftpSource channelSftpSource;

	@Pointcut("@annotation(com.quincy.sdk.annotation.ChannelSftpInjector)")
    public void pointCut() {}

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    	MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
    	Class<?>[] classes = methodSignature.getParameterTypes();
    	Object[] args = joinPoint.getArgs();
    	List<Integer> index = new ArrayList<Integer>(classes.length);
    	for(int i=0;i<classes.length;i++) {
    		Object arg = args[i];
    		if(ChannelSftp.class.getName().equals(classes[i].getName())&&(arg==null||AopHelper.isControllerMethod(joinPoint)))
    			index.add(i);
    	}
    	if(index.size()>0) {
    		ChannelSftp channel = null;
    		try {
    			channel = channelSftpSource.get();
    			for(int i:index)
    				args[i] = channel;
    			return joinPoint.proceed(args);
    		} finally {
    			if(channel!=null)
    				channel.disconnect();
    		}
    	} else
    		return joinPoint.proceed(args);
    }
}