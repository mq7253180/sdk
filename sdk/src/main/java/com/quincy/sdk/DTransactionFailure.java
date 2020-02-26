package com.quincy.sdk;

public interface DTransactionFailure {
	public int retriesBeforeInform();
	public void inform(String message);
}