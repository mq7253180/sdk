package com.quincy.core.aspect;

public class Cacheable {
	private long lastAccessTime;
	private long expireMillis;
	private Object value;

	public long getLastAccessTime() {
		return lastAccessTime;
	}
	public void setLastAccessTime(long lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}
	public long getExpireMillis() {
		return expireMillis;
	}
	public void setExpireMillis(long expireMillis) {
		this.expireMillis = expireMillis;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
}