package com.quincy.sdk.annotation.sharding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quincy.sdk.MasterOrSlave;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface ExecuteUpdate {
	public String sql();
	public MasterOrSlave masterOrSlave();
	public boolean anyway() default false;
}