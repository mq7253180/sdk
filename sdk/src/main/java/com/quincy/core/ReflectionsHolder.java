package com.quincy.core;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

public class ReflectionsHolder {
	private static Reflections reflections = null;
	private static Object lock = new Object();

	public static Reflections get() {
		if(reflections==null) {
			synchronized(lock) {
				if(reflections==null)
					reflections = new Reflections("com", Scanners.MethodsAnnotated, Scanners.TypesAnnotated, Scanners.SubTypes);
			}
		}
		return reflections;
	}
}