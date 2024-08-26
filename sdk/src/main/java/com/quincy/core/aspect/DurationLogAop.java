package com.quincy.core.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(1)
@Aspect
@Component
public class DurationLogAop {
	@Pointcut("@annotation(com.quincy.sdk.annotation.DurationLog)")
    public void pointCut() {}

	@Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		String fullMethodPath = CommonHelper.fullMethodPath(joinPoint, ".", "_", "#");
		long start = System.currentTimeMillis();
		Object toReturn = joinPoint.proceed();
		log.warn("DURATION=========================={}: {}", fullMethodPath, (System.currentTimeMillis()-start));
		return toReturn;
	}
}