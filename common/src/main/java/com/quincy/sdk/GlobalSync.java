package com.quincy.sdk;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GlobalSync {
	private static BlockingQueue<Runnable> blockingQueue = null;
	private static ThreadPoolExecutor threadPoolExecutor = null;

	static {
		if(blockingQueue==null)
			blockingQueue = new LinkedBlockingQueue<Runnable>(100000);
		if(threadPoolExecutor==null)
			threadPoolExecutor = new ThreadPoolExecutor(100, 100, 10, TimeUnit.SECONDS, blockingQueue);
	}

	public static ThreadPoolExecutor getThreadPoolExecutor() {
		return threadPoolExecutor;
	}
	public static BlockingQueue<Runnable> getBlockingQueue() {
		return blockingQueue;
	}
}
