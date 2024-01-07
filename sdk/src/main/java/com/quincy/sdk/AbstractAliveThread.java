package com.quincy.sdk;

public abstract class AbstractAliveThread extends Thread {
	private boolean loop = true;
	protected abstract void beforeLoop();
	protected abstract boolean doLoop();
	
	@Override
	public void run() {
		this.beforeLoop();
		while(loop) {
			if(this.doLoop())
				break;
		}
	}

	public void cancel() {
		this.loop = false;
	}
}