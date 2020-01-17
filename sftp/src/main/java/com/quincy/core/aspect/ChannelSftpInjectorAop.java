package com.quincy.core.aspect;

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
import com.quincy.sdk.annotation.ChannelSftpInjector;

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
    		Class<?>[] clazzes = methodSignature.getParameterTypes();
    		Object[] args = joinPoint.getArgs();
    		ChannelSftp channel = null;
    		try {
    			for(int i=0;i<clazzes.length;i++) {
    				Class<?> clazz = clazzes[i];
    				if(ChannelSftpInjector.class.getName().equals(clazz.getName())) {
    					channel = channelSftpSource.get();
    					args[i] = channel;
    					break;
    				}
    			}
    			return joinPoint.proceed(args);
    		} finally {
    			if(channel!=null)
    				channel.disconnect();
    		}
    }
}