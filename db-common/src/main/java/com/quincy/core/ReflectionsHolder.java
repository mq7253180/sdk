package com.quincy.core;

import org.reflections.Reflections;

public class ReflectionsHolder {
	private static Reflections reflections = new Reflections("");

	public static Reflections getReflections() {
		return reflections;
	}
}