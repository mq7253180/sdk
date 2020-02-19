package com.quincy.sdk;

public interface DistributedTransactionFailure {
	public int retriesBeforeInform();
	public void inform(String message);
}