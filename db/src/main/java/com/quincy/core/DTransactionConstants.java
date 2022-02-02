package com.quincy.core;

public class DTransactionConstants {
	public final static int ARG_TYPE_TX = 0;
	public final static int ARG_TYPE_ATOMIC = 1;
	public final static int ATOMIC_STATUS_INIT_FAILURE = 0;
	public final static int ATOMIC_STATUS_SUCCESS = 1;
	public final static int ATOMIC_STATUS_CANCELED = 2;
	public final static int TX_STATUS_ING = 0;
	public final static int TX_STATUS_ED = 1;
	public final static int TX_TYPE_CONFIRM = ATOMIC_STATUS_INIT_FAILURE;
	public final static int TX_TYPE_CANCEL = ATOMIC_STATUS_SUCCESS;
	public final static String REFERENCE_TO = "REFERENCE_TO: ";
}