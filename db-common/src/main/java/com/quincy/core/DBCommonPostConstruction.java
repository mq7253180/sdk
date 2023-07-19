package com.quincy.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.quincy.sdk.annotation.Column;
import com.quincy.sdk.annotation.DTO;

import jakarta.annotation.PostConstruct;

@Configuration
public class DBCommonPostConstruction {
	@Autowired
	private DataSource dataSource;
	@Autowired
	private TraditionalDaoConfiguration traditionalDaoConfiguration;
	private Map<Class<?>, Map<String, Method>> classMethodMap;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		Set<Class<?>> classes = ReflectionsHolder.getReflections().getTypesAnnotatedWith(DTO.class);
		this.classMethodMap = new HashMap<Class<?>, Map<String, Method>>();
		for(Class<?> clazz:classes) {
			Map<String, Method> subMap = new HashMap<String, Method>();
			Field[] fields = clazz.getDeclaredFields();
			for(Field field:fields) {
				Column column = field.getAnnotation(Column.class);
				if(column!=null) {
					String setterName = "set"+String.valueOf(field.getName().charAt(0)).toUpperCase()+field.getName().substring(1);
					subMap.put(column.value(), clazz.getMethod(setterName, field.getType()));
				}
			}
			this.classMethodMap.put(clazz, subMap);
		}
		traditionalDaoConfiguration.setClassMethodMap(this.classMethodMap);
		traditionalDaoConfiguration.setDataSource(dataSource);
	}

	public Map<Class<?>, Map<String, Method>> getClassMethodMap() {
		return classMethodMap;
	}
}