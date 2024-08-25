package com.quincy.auth.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.quincy.auth.interceptor.AuthorizationAnnotationInterceptor;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(AuthorizationAnnotationInterceptor.class)
public @interface EnableAnnotationAuth {	
}