package com.quincy.sdk.helper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public class AopHelper {
	public static Method getMethod(JoinPoint joinPoint) throws NoSuchMethodException, SecurityException {
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		Class<?> clazz = joinPoint.getTarget().getClass();
        return clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
	}

	public static <T extends Annotation> T getAnnotation(JoinPoint joinPoint, Class<T> annotationClass) throws NoSuchMethodException, SecurityException {
        return getMethod(joinPoint).getAnnotation(annotationClass);
	}
}
