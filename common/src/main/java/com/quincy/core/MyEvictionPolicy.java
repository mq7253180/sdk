package com.quincy.core;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.EvictionConfig;
import org.apache.commons.pool2.impl.EvictionPolicy;

public class MyEvictionPolicy<T> implements EvictionPolicy<T> {
	@Override
	public boolean evict(EvictionConfig config, PooledObject<T> underTest, int idleCount) {
		if (config.getIdleSoftEvictTime() < underTest.getIdleTimeMillis() && config.getMinIdle() < idleCount) {
            return true;
        }
		return false;
	}
}