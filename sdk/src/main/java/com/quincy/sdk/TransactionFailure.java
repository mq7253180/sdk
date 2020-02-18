package com.quincy.sdk;

public interface TransactionFailure {
	public int retriesBeforeInform();
	public void inform(String message);
}