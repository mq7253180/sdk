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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import com.quincy.core.db.RoutingDataSource;
import com.quincy.sdk.AllShardsDaoSupport;
import com.quincy.sdk.MasterOrSlave;
import com.quincy.sdk.annotation.sharding.AllShardsJDBCDao;
import com.quincy.sdk.annotation.sharding.ExecuteQuery;
import com.quincy.sdk.annotation.sharding.ExecuteUpdate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class GlobalShardingDaoConfiguration implements BeanDefinitionRegistryPostProcessor, AllShardsDaoSupport {
	private RoutingDataSource dataSource;
	private Map<Class<?>, Map<String, Method>> classMethodMap;
	private ThreadPoolExecutor threadPoolExecutor;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Set<Class<?>> classes = ReflectionsHolder.get().getTypesAnnotatedWith(AllShardsJDBCDao.class);
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

	public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
		this.threadPoolExecutor = threadPoolExecutor;
	}

	@Override
	public Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, MasterOrSlave masterOrSlave, Object... args)
			throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, InterruptedException, ExecutionException {
		return this.executeQuery(sql, returnType, returnItemType, masterOrSlave, System.currentTimeMillis(), args);
	}

	private Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, MasterOrSlave masterOrSlave, long start, Object[] args)
			throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, InterruptedException, ExecutionException {
		Map<String, Method> map = classMethodMap.get(returnItemType);
		int shardCount = dataSource.getResolvedDataSources().size()/2;
		boolean returnDto = returnType.getName().equals(returnItemType.getName());
		List<FutureTask<List<Object>>> tasks = new ArrayList<>(shardCount);
		for(int i=0;i<shardCount;i++) {
			int ii = i;
	        FutureTask<List<Object>> task = new FutureTask<>(new Callable<List<Object>>() {
				@Override
				public List<Object> call() throws Exception {
					List<Object> list =  new ArrayList<>(returnDto?1:10);
					String key = masterOrSlave.value()+ii;
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
							list.add(item);
							if(returnDto)
								return list;
//							if(returnDto) {
//								return item;
//							} else
//								list.add(item);
						}
//						if(!returnDto)
//							lists[i] = list;
						return list;
					} finally {
						log.warn("第{}个分片耗时========Duration============{}", ii, (System.currentTimeMillis()-start));
						if(rs!=null)
							rs.close();
						if(statment!=null)
							statment.close();
						if(conn!=null)
							conn.close();
					}
				}
	        });
	        tasks.add(task);
	        threadPoolExecutor.submit(task);
		}
		int completedCount = 0;
		boolean[] compleatedTasks = new boolean[shardCount];
		while(true) {
			for(int i=0;i<compleatedTasks.length;i++) {
				if(!compleatedTasks[i]) {
					FutureTask<List<Object>> task = tasks.get(i);
					if(task.isDone()) {
						compleatedTasks[i] = true;
						completedCount++;
					}
				}
			}
			if(completedCount>=shardCount)
				break;
			else
				Thread.sleep(10);
		}
//		return returnDto?null:lists;
		if(returnDto) {
			for(FutureTask<List<Object>> task:tasks) {
				List<Object> list = task.get();
				if(list!=null&&list.size()>0)
					return list.get(0);
			}
			return null;
		} else {
			@SuppressWarnings("unchecked")
			List<Object>[] lists = new ArrayList[shardCount];
			for(int i=0;i<tasks.size();i++) {
				FutureTask<List<Object>> task = tasks.get(i);
				lists[i] = task.get();
			}
			return lists;
		}
	}

	@Override
	public int[] executeUpdate(String sql, MasterOrSlave masterOrSlave, Object... args) throws SQLException, InterruptedException, ExecutionException {
		return this.executeUpdate(sql, masterOrSlave, System.currentTimeMillis(), args);
	}

	private int[] executeUpdate(String sql, MasterOrSlave masterOrSlave, long start, Object[] args) throws SQLException, InterruptedException, ExecutionException {
		int shardCount = dataSource.getResolvedDataSources().size()/2;
		List<FutureTask<Integer>> tasks = new ArrayList<>(shardCount);
		for(int i=0;i<shardCount;i++) {
			int ii = i;
	        FutureTask<Integer> task = new FutureTask<>(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					String key = masterOrSlave.value()+ii;
					Connection conn = null;
					PreparedStatement statment = null;
					try {
						conn = dataSource.getResolvedDataSources().get(key).getConnection();
						statment = conn.prepareStatement(sql);
						if(args!=null&&args.length>0) {
							for(int j=0;j<args.length;j++)
								statment.setObject(j+1, args[j]);
						}
						return statment.executeUpdate();
					} finally {
						log.warn("第{}个分片耗时========Duration============{}", ii, (System.currentTimeMillis()-start));
						if(statment!=null)
							statment.close();
						if(conn!=null)
							conn.close();
					}
				}
	        });
	        tasks.add(task);
			threadPoolExecutor.submit(task);
		}
		int completedCount = 0;
		boolean[] compleatedTasks = new boolean[shardCount];
		while(true) {
			for(int i=0;i<compleatedTasks.length;i++) {
				if(!compleatedTasks[i]) {
					FutureTask<Integer> task = tasks.get(i);
					if(task.isDone()) {
						compleatedTasks[i] = true;
						completedCount++;
					}
				}
			}
			if(completedCount>=shardCount)
				break;
			else
				Thread.sleep(10);
		}
		int[] toReturn = new int[shardCount];
		for(int i=0;i<tasks.size();i++) {
			FutureTask<Integer> task = tasks.get(i);
			toReturn[i] = task.get();
		}
		return toReturn;
	}
}