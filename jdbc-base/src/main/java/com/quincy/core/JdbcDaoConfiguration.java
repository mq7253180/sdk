package com.quincy.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.quincy.sdk.JdbcDao;
import com.quincy.sdk.annotation.ExecuteQuery;
import com.quincy.sdk.annotation.ExecuteUpdate;
import com.quincy.sdk.annotation.JDBCDao;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class JdbcDaoConfiguration implements BeanDefinitionRegistryPostProcessor, JdbcDao {
	private DataSource dataSource;
	private Map<Class<?>, Map<String, Method>> classMethodMap;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Set<Class<?>> classes = ReflectionsHolder.get().getTypesAnnotatedWith(JDBCDao.class);
		for(Class<?> clazz:classes) {
			Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {clazz}, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					long start = System.currentTimeMillis();
					ExecuteQuery queryAnnotation = method.getAnnotation(ExecuteQuery.class);
					ExecuteUpdate executeUpdateAnnotation = method.getAnnotation(ExecuteUpdate.class);
					Assert.isTrue(queryAnnotation!=null||executeUpdateAnnotation!=null, "What do you want to do?");
					Class<?> returnType = method.getReturnType();
					if(queryAnnotation!=null) {
						Class<?> returnItemType = queryAnnotation.returnItemType();
						Assert.isTrue(returnType.getName().equals(List[].class.getName())||returnType.getName().equals(ArrayList[].class.getName())||returnType.getName().equals(returnItemType.getName()), "Return type must be List[] or ArrayList[] or given returnItemType.");
						Object result = executeQuery(queryAnnotation.sql(), returnType, queryAnnotation.returnItemType(), args);
						log.warn("Duration======{}======{}", queryAnnotation.sql(), (System.currentTimeMillis()-start));
						return result;
					}
					if(executeUpdateAnnotation!=null) {
						Assert.isTrue(returnType.getName().equals(int.class.getName())||returnType.getName().equals(Integer.class.getName()), "Return type must be int[] or Integer[].");
						int rowCount = executeUpdate(executeUpdateAnnotation.sql(), args);
						log.warn("Duration======{}======{}", executeUpdateAnnotation.sql(), (System.currentTimeMillis()-start));
						return rowCount;
					}
					return null;
				}
			});
			String className = clazz.getName().substring(clazz.getName().lastIndexOf(".")+1);
			className = String.valueOf(className.charAt(0)).toLowerCase()+className.substring(1);
			beanFactory.registerSingleton(className, o);
		}
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setClassMethodMap(Map<Class<?>, Map<String, Method>> classMethodMap) {
		this.classMethodMap = classMethodMap;
	}

	@Override
	public Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, Object... args)
			throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		boolean returnDto = returnType.getName().equals(returnItemType.getName());
		List<Object> list = null;
		if(!returnDto)
			list = new ArrayList<>();
		Map<String, Method> map = classMethodMap.get(returnItemType);
		Object connectionHolder = TransactionSynchronizationManager.getResource(dataSource);
		Connection conn = connectionHolder==null?dataSource.getConnection():((ConnectionHolder)connectionHolder).getConnection();
		PreparedStatement statment = null;
		ResultSet rs = null;
		try {
			statment = conn.prepareStatement(sql);
			if(args!=null&&args.length>0) {
				for(int j=0;j<args.length;j++)
					statment.setObject(j+1, args[j]);
			}
			rs = statment.executeQuery();
			while(rs.next()) {
				Object item = returnItemType.getDeclaredConstructor().newInstance();
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				for(int j=1;j<=columnCount;j++) {
					String columnName = rsmd.getColumnLabel(j);
					Method setterMethod = map.get(columnName);
					if(setterMethod!=null) {
						Object v = rs.getObject(j);
						Class<?> parameterType = setterMethod.getParameterTypes()[0];
						if(!parameterType.isInstance(v)) {
							if(parameterType.getName().equals(String.class.getName())) {
								v = rs.getString(j);
							} else if(parameterType.getName().equals(boolean.class.getName())||parameterType.getName().equals(Boolean.class.getName())) {
								v = rs.getBoolean(j);
							} else if(parameterType.getName().equals(byte.class.getName())||parameterType.getName().equals(Byte.class.getName())) {
								v = rs.getByte(j);
							} else if(parameterType.getName().equals(short.class.getName())||parameterType.getName().equals(Short.class.getName())) {
								v = rs.getShort(j);
							} else if(parameterType.getName().equals(int.class.getName())||parameterType.getName().equals(Integer.class.getName())) {
								v = rs.getInt(j);
							} else if(parameterType.getName().equals(long.class.getName())||parameterType.getName().equals(Long.class.getName())) {
								v = rs.getLong(j);
							} else if(parameterType.getName().equals(float.class.getName())||parameterType.getName().equals(Float.class.getName())) {
								v = rs.getFloat(j);
							} else if(parameterType.getName().equals(double.class.getName())||parameterType.getName().equals(Double.class.getName())) {
								v = rs.getDouble(j);
							} else if(parameterType.getName().equals(BigDecimal.class.getName())) {
								v = rs.getBigDecimal(j);
							} else if(parameterType.getName().equals(Date.class.getName())||parameterType.getName().equals(java.sql.Date.class.getName())) {
								v = rs.getDate(j);
							} else if(parameterType.getName().equals(Time.class.getName())) {
								v = rs.getTime(j);
							} else if(parameterType.getName().equals(Timestamp.class.getName())) {
								v = rs.getTimestamp(j);
							} else if(parameterType.getName().equals(Array.class.getName())) {
								v = rs.getArray(j);
							} else if(parameterType.getName().equals(Blob.class.getName())) {
								v = rs.getBlob(j);
							} else if(parameterType.getName().equals(Clob.class.getName())) {
								v = rs.getClob(j);
							} else if(parameterType.getName().equals(byte[].class.getName())) {
								InputStream in = null;
								try {
									in = rs.getBinaryStream(j);
									byte[] buf = new byte[in.available()];
									in.read(buf);
									v = buf;
								} finally {
									if(in!=null)
										in.close();
								}
							}
						}
						setterMethod.invoke(item, v);
					}
				}
				if(returnDto) {
					return item;
				} else
					list.add(item);
			}
			return returnDto?null:list;
		} finally {
			if(rs!=null)
				rs.close();
			if(statment!=null)
				statment.close();
			if(connectionHolder==null&&conn!=null)
				conn.close();
		}
	}

	@Override
	public int executeUpdate(String sql, Object... args) throws SQLException {
		Object connectionHolder = TransactionSynchronizationManager.getResource(dataSource);
		Connection conn = connectionHolder==null?dataSource.getConnection():((ConnectionHolder)connectionHolder).getConnection();
		PreparedStatement statment = null;
		try {
			statment = conn.prepareStatement(sql);
			if(args!=null&&args.length>0) {
				for(int j=0;j<args.length;j++)
					statment.setObject(j+1, args[j]);
			}
			return statment.executeUpdate();
		} finally {
			if(statment!=null)
				statment.close();
			if(connectionHolder==null&&conn!=null)
				conn.close();
		}
	}
}