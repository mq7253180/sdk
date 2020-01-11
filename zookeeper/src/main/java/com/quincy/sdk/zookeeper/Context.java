package com.quincy.sdk.zookeeper;

public interface Context {
	public void addHandler(Handler h);
	public boolean handlerExists(String path);
	public String getRootPath();
	public String getSynPath();
}