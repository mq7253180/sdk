package com.quincy.sdk.annotation.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quincy.core.InnerConstants;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface DTransactional {
	public String frequencyBatch() default "";
	public boolean inOrder() default false;
	public boolean async() default true;
	public String executor() default InnerConstants.BEAN_NAME_SYS_THREAD_POOL;
}