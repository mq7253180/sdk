package com.quincy.core;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.quincy.core.aspect.DistributedTransactionAop;
import com.quincy.sdk.ShardingDTranactionOptRegistry;

@Component
public class ShardingDTranactionAdapter extends DistributedTransactionAop implements ShardingDTranactionOptRegistry {
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	@Override
	public void resume(Integer shardingKey)
			throws ClassNotFoundException, NoSuchMethodException, IOException, InterruptedException {
		this.resume();
	}

	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	@Override
	public void resume(Integer shardingKey, String flagForCronJob)
			throws ClassNotFoundException, NoSuchMethodException, IOException, InterruptedException {
		this.resume(flagForCronJob);
	}
}