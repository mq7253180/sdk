package com.quincy.sdk.annotation.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.quincy.auth.SessionDestroyedOriginalConfiguration;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(SessionDestroyedOriginalConfiguration.class)
public @interface EnableOriginalSessionEviction {
	boolean browser() default false;
	boolean app() default false;
}