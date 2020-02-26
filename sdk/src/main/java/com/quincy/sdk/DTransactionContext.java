package com.quincy.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface DTransactionContext {
	public void setTransactionFailure(DTransactionFailure transactionFailure);
	public void compensate() throws JsonMappingException, ClassNotFoundException, JsonProcessingException, NoSuchMethodException, SecurityException;
}