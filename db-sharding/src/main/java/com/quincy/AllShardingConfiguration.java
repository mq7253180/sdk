package com.quincy;

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

import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import com.quincy.core.TraditionalDaoConfiguration;
import com.quincy.core.db.RoutingDataSource;
import com.quincy.sdk.AllShardsDBCSupport;
import com.quincy.sdk.MasterOrSlave;
import com.quincy.sdk.annotation.sharding.AllShardsJDBCDao;
import com.quincy.sdk.annotation.sharding.ExecuteQuery;
import com.quincy.sdk.annotation.sharding.ExecuteUpdate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class AllShardingConfiguration implements BeanDefinitionRegistryPostProcessor, AllShardsDBCSupport {
	private RoutingDataSource dataSource;
	private Map<Class<?>, Map<String, Method>> classMethodMap;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		TraditionalDaoConfiguration.initReflections();
		Reflections reflections = TraditionalDaoConfiguration.getReflections();
		Set<Class<?>> classes = reflections.getTypesAnnotatedWith(AllShardsJDBCDao.class);
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
						return executeQuery(queryAnnotation.sql(), returnType, returnItemType, queryAnnotation.masterOrSlave(), args);
					}
					if(executeUpdateAnnotation!=null) {
						Assert.isTrue(returnType.getName().equals(int[].class.getName())||returnType.getName().equals(Integer[].class.getName()), "Return type must be int[] or Integer[].");
						return executeUpdate(executeUpdateAnnotation.sql(), executeUpdateAnnotation.masterOrSlave(), start, args);
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

	public void setDataSource(RoutingDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setClassMethodMap(Map<Class<?>, Map<String, Method>> classMethodMap) {
		this.classMethodMap = classMethodMap;
	}

	@Override
	public Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, MasterOrSlave masterOrSlave, Object[] args)
			throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		return this.executeQuery(sql, returnType, returnItemType, masterOrSlave, System.currentTimeMillis(), args);
	}

	private Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, MasterOrSlave masterOrSlave, long start, Object[] args)
			throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		Map<String, Method> map = classMethodMap.get(returnItemType);
		int shardCount = dataSource.getResolvedDataSources().size()/2;
		@SuppressWarnings("unchecked")
		List<Object>[] lists = new ArrayList[shardCount];
		for(int i=0;i<shardCount;i++) {
			List<Object> list = new ArrayList<>();
			String key = masterOrSlave.value()+i;
			Connection conn = null;
			PreparedStatement statment = null;
			ResultSet rs = null;
			try {
				conn = dataSource.getResolvedDataSources().get(key).getConnection();
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
					if(returnType.getName().equals(returnItemType.getName())) {
						return item;
					} else
						list.add(item);
				}
				lists[i] = list;
			} finally {
				log.warn("第{}个分片耗时========Duration============{}", i, (System.currentTimeMillis()-start));
				if(rs!=null)
					rs.close();
				if(statment!=null)
					statment.close();
				if(conn!=null)
					conn.close();
			}
		}
		return lists;
	}

	@Override
	public int[] executeUpdate(String sql, MasterOrSlave masterOrSlave, Object[] args) throws SQLException {
		return this.executeUpdate(sql, masterOrSlave, System.currentTimeMillis(), args);
	}

	private int[] executeUpdate(String sql, MasterOrSlave masterOrSlave, long start, Object[] args) throws SQLException {
		int shardCount = dataSource.getResolvedDataSources().size()/2;
		int[] toReturn = new int[shardCount];
		for(int i=0;i<shardCount;i++) {
			String key = masterOrSlave.value()+i;
			Connection conn = null;
			PreparedStatement statment = null;
			try {
				conn = dataSource.getResolvedDataSources().get(key).getConnection();
				statment = conn.prepareStatement(sql);
				if(args!=null&&args.length>0) {
					for(int j=0;j<args.length;j++)
						statment.setObject(j+1, args[j]);
				}
				toReturn[i] = statment.executeUpdate();
			} finally {
				log.warn("第{}个分片耗时========Duration============{}", i, (System.currentTimeMillis()-start));
				if(statment!=null)
					statment.close();
				if(conn!=null)
					conn.close();
			}
		}
		return toReturn;
	}
}