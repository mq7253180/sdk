package com.quincy.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import com.quincy.core.db.JdbcDaoConstants;
import com.quincy.sdk.annotation.Column;
import com.quincy.sdk.annotation.DTO;
import com.quincy.sdk.annotation.DynamicColumns;

import jakarta.annotation.PostConstruct;

@Configuration
public class JdbcPostConstruction {
	@Autowired
	private DataSource dataSource;
	@Autowired
	private JdbcDaoConfiguration jdbcDaoConfiguration;
	private Map<Class<?>, Map<String, Method>> classMethodMap;

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		Set<Class<?>> classes = ReflectionsHolder.get().getTypesAnnotatedWith(DTO.class);
		this.classMethodMap = new HashMap<Class<?>, Map<String, Method>>();
		for(Class<?> clazz:classes) {
			Map<String, Method> subMap = new HashMap<String, Method>();
			Field[] fields = clazz.getDeclaredFields();
			for(Field field:fields) {
				Column column = field.getAnnotation(Column.class);
				String setterKey = null;
				String getterKey = null;
				if(column!=null) {
					setterKey = column.value();
				} else {
					DynamicColumns dynamicColumns = field.getAnnotation(DynamicColumns.class);
					if(dynamicColumns!=null) {
						Assert.isTrue(field.getType().getName().equals(List.class.getName())||field.getType().getName().equals(ArrayList.class.getName()), field.getName()+" must be List or ArrayList.");
						setterKey = JdbcDaoConstants.DYNAMIC_COLUMN_LIST_SETTER_METHOD_KEY;
						getterKey = JdbcDaoConstants.DYNAMIC_COLUMN_LIST_GETTER_METHOD_KEY;
					}
				}
				String fieldNameByFistUpperCase = String.valueOf(field.getName().charAt(0)).toUpperCase()+field.getName().substring(1);
				if(setterKey!=null) {
					String setterName = "set"+fieldNameByFistUpperCase;
					subMap.put(setterKey, clazz.getMethod(setterName, field.getType()));
				}
				if(getterKey!=null) {
					String getterName = "get"+fieldNameByFistUpperCase;
					subMap.put(getterKey, clazz.getMethod(getterName));
				}
			}
			this.classMethodMap.put(clazz, subMap);
		}
		jdbcDaoConfiguration.setClassMethodMap(this.classMethodMap);
		jdbcDaoConfiguration.setDataSource(dataSource);
	}

	public Map<Class<?>, Map<String, Method>> getClassMethodMap() {
		return classMethodMap;
	}
}