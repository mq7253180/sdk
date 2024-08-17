package com.quincy.sdk;

import java.io.IOException;

public interface DTransactionOptRegistry {
	public void setTransactionFailure(DTransactionFailure transactionFailure);
	public void resume() throws ClassNotFoundException, NoSuchMethodException, IOException, InterruptedException;
	public void resume(String flagForCronJob) throws ClassNotFoundException, NoSuchMethodException, IOException, InterruptedException;
}
