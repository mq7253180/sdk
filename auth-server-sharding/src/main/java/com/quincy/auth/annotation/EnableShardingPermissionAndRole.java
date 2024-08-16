package com.quincy.auth.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.quincy.auth.service.impl.XSessionServiceShardingProxyImpl;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(XSessionServiceShardingProxyImpl.class)
public @interface EnableShardingPermissionAndRole {
}