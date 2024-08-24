package com.quincy.core.aspect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.sdk.annotation.PrimaryCache;
import com.quincy.sdk.helper.CommonHelper;

@Aspect
@Order(1)
@Component
public class PrimaryCacheAop {
	private final static Map<String, Cacheable> CACHE = new ConcurrentHashMap<String, Cacheable>();
	private final static Timer timer = new Timer();
	static {
		timer.schedule(new Evictor(), 5000, 5000);
	}

	private static class Evictor extends TimerTask {
		@Override
		public void run() {
			long currentTimeMillis = System.currentTimeMillis();
			Set<Entry<String, Cacheable>> entries = CACHE.entrySet();
			for(Entry<String, Cacheable> e:entries) {
				Cacheable c = e.getValue();
				if(currentTimeMillis-c.getLastAccessTime()>=c.getExpireMillis())
					CACHE.remove(e.getKey());
			}
		}
	}

	@Pointcut("@annotation(com.quincy.sdk.annotation.PrimaryCache)")
    public void pointCut() {}

	@Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Class<?> clazz = joinPoint.getTarget().getClass();
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
		String key = CommonHelper.fullMethodPath(clazz, methodSignature, method, joinPoint.getArgs(), ".", "_", "#");
		Object value = null;
		Cacheable cacheable = CACHE.get(key);
		if(cacheable==null) {
			PrimaryCache annotation = method.getAnnotation(PrimaryCache.class);
			value = joinPoint.proceed();
			cacheable = new Cacheable();
			cacheable.setValue(value);
			cacheable.setExpireMillis(annotation.expire()*1000);
			CACHE.put(key, cacheable);
		}
		cacheable.setLastAccessTime(System.currentTimeMillis());
		return cacheable.getValue();
	}
}