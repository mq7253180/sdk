package com.quincy.sdk.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cache {
	public int expire() default 180;//默认3分钟
	public int retries() default 3;//抢锁失败功后尝试重新获取缓存次数
	public long millisBetweenRetries() default 500;//抢锁失败功后每次尝试间隔毫秒
}