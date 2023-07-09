package com.quincy.sdk;

public enum MasterOrSlave {
	MASTER("master"), SLAVE("slave");
	
	private String value;

	private MasterOrSlave(String value) {
		this.value = value;
	}

	public String value() {
		return this.value;
	}
}