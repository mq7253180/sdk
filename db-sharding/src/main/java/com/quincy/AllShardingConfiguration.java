package com.quincy;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
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

import com.quincy.core.db.RoutingDataSource;
import com.quincy.sdk.annotation.AllShardSQL;
import com.quincy.sdk.annotation.ExecuteQuery;

@Configuration
public class AllShardingConfiguration implements BeanDefinitionRegistryPostProcessor {
	private RoutingDataSource dataSource;
	private Map<Class<?>, Map<String, Method>> classMethodMap;
	private Reflections reflections;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.reflections = new Reflections("");
		Set<Class<?>> classes = this.reflections.getTypesAnnotatedWith(AllShardSQL.class);
		for(Class<?> clazz:classes) {
			Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {clazz}, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					long start = System.currentTimeMillis();
					Class<?> returnType = method.getReturnType();
					Assert.isTrue(returnType.getName().equals(List.class.getName())||returnType.getName().equals(ArrayList.class.getName()), "Return type must be List or ArrayList.");
					returnType.getGenericInterfaces();
					ExecuteQuery annotation = method.getAnnotation(ExecuteQuery.class);
					if(annotation!=null) {
						String sql = annotation.sql();
						Class<?> returnItemType = annotation.returnItemType();
						Map<String, Method> map = classMethodMap.get(returnItemType);
						List<Object> list = new ArrayList<>();
						int shardCount = dataSource.getResolvedDataSources().size()/2;
						System.out.println("Duration1===================="+(System.currentTimeMillis()-start));
						for(int i=0;i<shardCount;i++) {
							String key = annotation.masterOrSlave().value()+i;
							Connection conn = null;
							PreparedStatement statment = null;
							try {
								conn = dataSource.getResolvedDataSources().get(key).getConnection();
								statment = conn.prepareStatement(sql);
//								statment.setString(0, sql);
								ResultSet rs = statment.executeQuery();
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
									list.add(item);
								}
								statment.close();
							} finally {
								if(statment!=null)
									statment.close();
								if(conn!=null)
									conn.close();
							}
						}
						System.out.println("Duration2===================="+(System.currentTimeMillis()-start));
						return list;
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

	public Reflections getReflections() {
		return reflections;
	}
}