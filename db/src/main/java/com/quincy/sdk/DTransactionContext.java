package com.quincy.sdk;

import java.io.IOException;

public interface DTransactionContext {
	public void setTransactionFailure(DTransactionFailure transactionFailure);
	public void compensate() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IOException;
	public void compensate(String frequencyBatch) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IOException;
}