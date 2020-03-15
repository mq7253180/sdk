package com.quincy.sdk;

public enum Client {
	PC(1, "pc"), WAP(2, "wap"), APP(3, "app");

	private int code;
	private String name;

	private Client(int code, String name) {
		this.code = code;
		this.name = name;
	}

	public Client get(int code) {
		for (Client c : Client.values()) { 
			if(c.getCode()==code)
				return c;
		}
		return null;
	}

	public int getCode() {
		return code;
	}
	public String getName() {
		return this.name;
	}
}