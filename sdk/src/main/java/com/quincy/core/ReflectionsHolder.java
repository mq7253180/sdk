package com.quincy.core;

import org.reflections.Reflections;

public class ReflectionsHolder {
	private static Reflections reflections = null;
	private static Object lock = new Object();

	public static Reflections get() {
		if(reflections==null) {
			synchronized(lock) {
				if(reflections==null)
					reflections = new Reflections("com");
			}
		}
		return reflections;
	}
}