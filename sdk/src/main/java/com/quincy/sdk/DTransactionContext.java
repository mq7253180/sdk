package com.quincy.sdk;

import java.io.IOException;

public interface DTransactionContext {
	public void setTransactionFailure(DTransactionFailure transactionFailure);
	public void compensate() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IOException;
}