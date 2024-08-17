package com.quincy.sdk;

import java.io.IOException;

public interface DTransactionContext {
	public void setTransactionFailure(DTransactionFailure transactionFailure);
	public void resume() throws ClassNotFoundException, NoSuchMethodException, IOException, InterruptedException;
	public void resume(String frequencyBatch) throws ClassNotFoundException, NoSuchMethodException, IOException, InterruptedException;
}
