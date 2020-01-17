package com.quincy.core.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import com.quincy.sdk.annotation.DeprecatedSynchronized;
import com.quincy.sdk.helper.AopHelper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

@Aspect
@Order(2)
@Component
public class DeprecatedSynchronizedAop extends JedisNeededBaseAop {
	private final static String SET_KEY = "SYNCHRONIZATION";

	@Pointcut("@annotation(com.quincy.sdk.annotation.DeprecatedSynchronized)")
    public void pointCut() {}

	@Override
	protected Object before(JoinPoint joinPoint, Jedis jedis) throws NoSuchMethodException, SecurityException, InterruptedException {
		DeprecatedSynchronized annotation = AopHelper.getAnnotation(joinPoint, DeprecatedSynchronized.class);
		String key = annotation.value();
		String channels = SET_KEY+"_"+key;
		while(true) {
			if(jedis.sismember(SET_KEY, key)||jedis.sadd(SET_KEY, key)==0)
				jedis.subscribe(new MyListener(), channels);
			else //Successfully held the distributed lock.
				break;
		}
		return key;
	}

	@Override
	protected void after(JoinPoint joinPoint, Jedis jedis, Object obj) {
		String key = obj.toString();
		jedis.srem(SET_KEY, key);
		jedis.publish(SET_KEY+"_"+key, "Finished");
	}

	public class MyListener extends JedisPubSub {
		public void onMessage(String channel, String message) {
			this.unsubscribe();
		}
	}
}