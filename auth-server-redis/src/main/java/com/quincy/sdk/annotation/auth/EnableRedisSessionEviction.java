package com.quincy.sdk.annotation.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.quincy.core.SessionDestroyedRedisConfiguration;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(SessionDestroyedRedisConfiguration.class)
public @interface EnableRedisSessionEviction {
	boolean pcBrowser() default false;
	boolean mobileBrowser() default false;
	boolean app() default false;
}