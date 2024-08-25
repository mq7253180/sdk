package com.quincy.core.aspect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.core.ReflectionsHolder;
import com.quincy.sdk.annotation.PrimaryCache;
import com.quincy.sdk.helper.CommonHelper;

@Aspect
@Order(2)
@Component
public class PrimaryCacheAop {
	private final static Map<String, Cacheable> STORAGE = new ConcurrentHashMap<String, Cacheable>();
	private final static AtomicInteger LOCK = new AtomicInteger();
	private static Timer timer = null;
	@Value("${cache.primary.evictor.delay:5000}")
	private long evictorDelay;
	@Value("${cache.primary.evictor.period:5000}")
	private long evictorPeriod;

	@PostConstruct
	public void init() {
		Set<Method> methods = ReflectionsHolder.get().getMethodsAnnotatedWith(PrimaryCache.class);
		if(methods!=null&&methods.size()>0) {
			timer = new Timer();
			timer.schedule(new Evictor(), evictorDelay, evictorPeriod);
		}
	}

	private static class Evictor extends TimerTask {
		@Override
		public void run() {
			if(STORAGE.size()>0) {
				long currentTimeMillis = System.currentTimeMillis();
				Set<Entry<String, Cacheable>> entries = STORAGE.entrySet();
				for(Entry<String, Cacheable> e:entries) {
					Cacheable c = e.getValue();
					if(currentTimeMillis-c.getLastAccessTime()>=c.getExpireMillis())
						STORAGE.remove(e.getKey());
						/*System.out.println("REMOTED================"+e.getKey());
					} else
						System.out.println("------------------LOOPING");*/
				}
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
		Cacheable cacheable = STORAGE.get(key);
		if(cacheable==null) {
			PrimaryCache annotation = method.getAnnotation(PrimaryCache.class);
			if(LOCK.compareAndSet(0, 1)) {
				cacheable = this.invokeAndCache(joinPoint, annotation, key);
				LOCK.set(0);
			} else {
				for(int i=0;i<annotation.retries();i++) {
					Thread.sleep(annotation.millisBetweenRetries());
					cacheable = STORAGE.get(key);
					if(cacheable!=null)
						break;
				}
				if(cacheable==null&&!annotation.returnNull())
					return this.invokeAndCache(joinPoint, annotation, key);
			}
		}
		if(cacheable==null) {
			return null;
		} else {
			cacheable.setLastAccessTime(System.currentTimeMillis());
			return cacheable.getValue();
		}
	}

	private Cacheable invokeAndCache(ProceedingJoinPoint joinPoint, PrimaryCache annotation, String key) throws Throwable {
		Cacheable cacheable = new Cacheable();
		cacheable.setValue(joinPoint.proceed());
		cacheable.setExpireMillis(annotation.expire()*1000);
		STORAGE.put(key, cacheable);
		return cacheable;
	}

	private class Cacheable {
		private long lastAccessTime;
		private long expireMillis;
		private Object value;

		public long getLastAccessTime() {
			return lastAccessTime;
		}
		public void setLastAccessTime(long lastAccessTime) {
			this.lastAccessTime = lastAccessTime;
		}
		public long getExpireMillis() {
			return expireMillis;
		}
		public void setExpireMillis(long expireMillis) {
			this.expireMillis = expireMillis;
		}
		public Object getValue() {
			return value;
		}
		public void setValue(Object value) {
			this.value = value;
		}
	}
}