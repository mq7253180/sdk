package com.quincy.sdk.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface Cache {
	public int expire() default 180;
	public String key() default "";
	public int setnxExpire() default 3;
	public int notExistRetries() default 3;
	public long notExistSleepMillis() default 500;
	public boolean returnNull() default true;
}