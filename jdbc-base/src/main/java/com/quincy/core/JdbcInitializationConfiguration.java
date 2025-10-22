package com.quincy.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.Assert;

import com.quincy.core.db.JdbcDaoConstants;
import com.quincy.sdk.annotation.jdbc.Column;
import com.quincy.sdk.annotation.jdbc.DTO;
import com.quincy.sdk.annotation.jdbc.DynamicColumnQueryDTO;
import com.quincy.sdk.annotation.jdbc.DynamicColumns;
import com.quincy.sdk.annotation.jdbc.DynamicFields;
import com.quincy.sdk.annotation.jdbc.Result;

@PropertySource("classpath:application-jdbc.properties")
@Configuration(JdbcHolder.INIT_CONFIGURATION_BEAN_NAME)
public class JdbcInitializationConfiguration {
	@Autowired
	private DataSource dataSource;
	@Autowired
	private JdbcDaoConfiguration jdbcDaoConfiguration;
	private Map<Class<?>, Map<String, Method>> classMethodMap = new HashMap<Class<?>, Map<String, Method>>();
	private Map<Class<?>, Class<?>> returnTypeMap = new HashMap<Class<?>, Class<?>>();

	@PostConstruct
	public void init() throws NoSuchMethodException, SecurityException {
		this.doLoop(DTO.class, new ForOperation() {
			@Override
			public Class<?> innerLoop(Class<?> clazz, Field field, Map<String, Method> subMap) throws NoSuchMethodException, SecurityException {
				Column columnAnnotation = field.getAnnotation(Column.class);
				String setterKey = null;
				String getterKey = null;
				if(columnAnnotation!=null) {
					setterKey = columnAnnotation.value();
				} else {
					DynamicColumns dynamicColumnsAnnotation = field.getAnnotation(DynamicColumns.class);
					if(dynamicColumnsAnnotation!=null) {
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
				return null;
			}
		});
		this.doLoop(DynamicColumnQueryDTO.class, new ForOperation() {
			@Override
			public Class<?> innerLoop(Class<?> clazz, Field field, Map<String, Method> subMap) throws NoSuchMethodException, SecurityException {
				DynamicFields dynamicFieldsAnnotation = field.getAnnotation(DynamicFields.class);
				if(dynamicFieldsAnnotation!=null)
					subMap.put(JdbcDaoConstants.DYNAMIC_COLUMN_WRAPPER_FIELDS_SETTER_METHOD_KEY, clazz.getMethod("set"+String.valueOf(field.getName().charAt(0)).toUpperCase()+field.getName().substring(1), field.getType()));
				Class<?> resultType = null;
				Result resultAnnotation = field.getAnnotation(Result.class);
				if(resultAnnotation!=null) {
					String name = String.valueOf(field.getName().charAt(0)).toUpperCase()+field.getName().substring(1);
					subMap.put(JdbcDaoConstants.DYNAMIC_COLUMN_WRAPPER_RESULT_SETTER_METHOD_KEY, clazz.getMethod("set"+name, field.getType()));
					resultType = field.getType();
				}
				return resultType;
			}
		});
		jdbcDaoConfiguration.setClassMethodMap(this.classMethodMap);
		jdbcDaoConfiguration.setReturnTypeMap(this.returnTypeMap);
		jdbcDaoConfiguration.setDataSource(dataSource);
	}

	private void doLoop(Class<? extends Annotation> annotation, ForOperation forOperation) throws NoSuchMethodException, SecurityException {
		Set<Class<?>> classes = ReflectionsHolder.get().getTypesAnnotatedWith(annotation);
		for(Class<?> clazz:classes) {
			Map<String, Method> subMap = new HashMap<String, Method>();
			Field[] fields = clazz.getDeclaredFields();
			Class<?> returnType = null;
			for(Field field:fields) {
				Class<?> tempRetType = forOperation.innerLoop(clazz, field, subMap);
				if(tempRetType!=null)
					returnType = tempRetType;
			}
			this.classMethodMap.put(clazz, subMap);
			if(returnType!=null)
				this.returnTypeMap.put(clazz, returnType);
		}
	}

	private interface ForOperation {
		public Class<?> innerLoop(Class<?> clazz, Field field, Map<String, Method> subMap) throws NoSuchMethodException, SecurityException;
	}

	public Map<Class<?>, Map<String, Method>> getClassMethodMap() {
		return classMethodMap;
	}
}