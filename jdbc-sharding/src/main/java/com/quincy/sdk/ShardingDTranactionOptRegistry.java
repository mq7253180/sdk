package com.quincy.sdk;

import java.io.IOException;

public interface ShardingDTranactionOptRegistry {
	public void setTransactionFailure(DTransactionFailure transactionFailure);
	public void resume(Integer shardingKey) throws ClassNotFoundException, NoSuchMethodException, IOException, InterruptedException;
	public void resume(Integer shardingKey, String flagForCronJob) throws ClassNotFoundException, NoSuchMethodException, IOException, InterruptedException;
}