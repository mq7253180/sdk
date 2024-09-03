package com.quincy.core.aspect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.core.ReflectionsHolder;
import com.quincy.sdk.annotation.Cache;
import com.quincy.sdk.helper.CommonHelper;

@Order(2)
@Aspect
@Component
public class PrimaryCacheAop {
	private final static Log log = LogFactory.getLog(PrimaryCacheAop.class);
	private static Map<String, Cacheable> STORAGE = null;
	private static Map<String, Lockable> LOCKS_HOLDER = null;
	@Value("${cache.primary.evictor.delay:5000}")
	private long evictorDelay;
	@Value("${cache.primary.evictor.period:5000}")
	private long evictorPeriod;

	@PostConstruct
	public void init() {
		Set<Method> methods = ReflectionsHolder.get().getMethodsAnnotatedWith(Cache.class);
		if(methods!=null&&methods.size()>0) {
			STORAGE = new ConcurrentHashMap<String, Cacheable>();
			LOCKS_HOLDER = new HashMap<String, Lockable>();
			for(Method method:methods)//不同类的同名方法、同方法的不同参数也会共用同一把锁
				LOCKS_HOLDER.put(method.getName(), new Lockable(new AtomicInteger(), new Object()));
			new Timer().schedule(new Evictor(), evictorDelay, evictorPeriod);
		}
	}

	private static class Evictor extends TimerTask {
		@Override
		public void run() {
			if(STORAGE.size()>0) {
				long currentTimeMillis = System.currentTimeMillis();
				Set<Entry<String, Cacheable>> entries = STORAGE.entrySet();
				for(Entry<String, Cacheable> e:entries) {
					Cacheable cacheable = e.getValue();
					if(currentTimeMillis-cacheable.getLastAccessTime()>=cacheable.getExpireMillis()) {
						STORAGE.remove(e.getKey());
						log.info("REMOTED================"+e.getKey());
					} else
						log.info("------------------LOOPING");
				}
			}
		}
	}

	@Pointcut("@annotation(com.quincy.sdk.annotation.Cache)")
    public void pointCut() {}

	@Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Class<?> clazz = joinPoint.getTarget().getClass();
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
		String key = CommonHelper.fullMethodPath(clazz, methodSignature, method, joinPoint.getArgs(), ".", "_", "#");
		Cacheable cacheable = STORAGE.get(key);
		if(cacheable==null) {
			Cache annotation = method.getAnnotation(Cache.class);
			Lockable lockable = LOCKS_HOLDER.get(method.getName());
			AtomicInteger nx = lockable.getNx();
			Object lock = lockable.getLock();
			for(int i=0;i<=annotation.retries();i++) {
				if(nx.compareAndSet(0, 1)) {
					log.info(Thread.currentThread().threadId()+"------------------获取锁");
					cacheable = STORAGE.get(key);
					if(cacheable==null) {
						cacheable = this.invokeAndCache(joinPoint, annotation, key);
						synchronized(lock) {
							lock.notifyAll();
						}
					}
					nx.set(0);
					break;
				} else {
					synchronized(lock) {
						log.info(Thread.currentThread().threadId()+"----------第"+i+"次--------等待");
						lock.wait(annotation.millisBetweenRetries());
					}
					cacheable = STORAGE.get(key);
					if(cacheable!=null) {//被其他线程查库赋值，获取数据
						log.info(Thread.currentThread().threadId()+"---------第"+i+"次---------获取数据");
						break;
					}
				}
			}
			if(cacheable==null)//重试次数后没有其他线程查库赋值
				cacheable = this.invokeAndCache(joinPoint, annotation, key);
		}
		cacheable.setLastAccessTime(System.currentTimeMillis());
		return cacheable.getValue();
	}

	private Cacheable invokeAndCache(ProceedingJoinPoint joinPoint, Cache annotation, String key) throws Throwable {
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

	private class Lockable {
		private AtomicInteger nx;
		private Object lock;

		private Lockable(AtomicInteger nx, Object lock) {
			this.nx = nx;
			this.lock = lock;
		}
		public AtomicInteger getNx() {
			return nx;
		}
		public Object getLock() {
			return lock;
		}
	}
}