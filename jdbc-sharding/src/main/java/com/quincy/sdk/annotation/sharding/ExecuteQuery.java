package com.quincy.sdk.annotation.sharding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quincy.sdk.MasterOrSlave;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExecuteQuery {
	public String sql();
	public MasterOrSlave masterOrSlave() default MasterOrSlave.SLAVE;
	public Class<?> returnItemType();
	public boolean anyway() default false;
}