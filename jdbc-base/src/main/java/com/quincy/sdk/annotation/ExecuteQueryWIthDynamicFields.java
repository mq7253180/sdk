package com.quincy.sdk.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface ExecuteQueryWIthDynamicFields {
	/**
	 * f.name,f.sort,v.value_decimal and primary key of business data as id must be presented in sqlFrontHalf.
	 */
	public String sqlFrontHalf();
	public Class<?> returnItemType();
	public String tableName();
}