package com.quincy.sdk.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface Cache {
	public int expire() default 60;
	public String key() default "";
	public int setnxDelaySecs() default 3;
	public int setnxFailRetries() default 3;
	public long intervalMillis() default 1000;
	public boolean returnNull() default true;
}