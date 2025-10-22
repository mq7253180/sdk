package com.quincy.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
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

import com.quincy.sdk.ShardingJdbcDao;
import com.quincy.sdk.SnowFlake;
import com.quincy.core.db.RoutingDataSource;
import com.quincy.sdk.MasterOrSlave;
import com.quincy.sdk.annotation.sharding.AllShardsJDBCDao;
import com.quincy.sdk.annotation.sharding.ExecuteQuery;
import com.quincy.sdk.annotation.sharding.ExecuteUpdate;
import com.quincy.sdk.annotation.sharding.ShardingKeyToSkip;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ShardingJdbcDaoConfiguration implements BeanDefinitionRegistryPostProcessor, ShardingJdbcDao {
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
						return executeQuery(queryAnnotation.sql(), returnType, returnItemType, queryAnnotation.masterOrSlave(), queryAnnotation.anyway(), args);
					}
					if(executeUpdateAnnotation!=null) {
						Assert.isTrue(returnType.getName().equals(int[].class.getName())||returnType.getName().equals(Integer[].class.getName()), "Return type must be int[] or Integer[].");
						Annotation[][] annotationss = method.getParameterAnnotations();
						int indexOfShardingKeyToSkip = -1;
						boolean snowFlake = false;
						for(int i=0;i<annotationss.length;i++) {
							Annotation[] annotations = annotationss[i];
							boolean stopLoop = false;
			    			for(int j=0;j<annotations.length;j++) {
			    				if(annotations[j] instanceof ShardingKeyToSkip) {
			    					ShardingKeyToSkip annotation = (ShardingKeyToSkip)annotations[j];
			    					snowFlake = annotation.snowFlake();
			    					indexOfShardingKeyToSkip = i;
			    					stopLoop = true;
					    			break;
					    		}
			    			}
			    			if(stopLoop)
			    				break;
						}
						long shardingKey = -1;
						Object[] afterArgs = args;
						if(indexOfShardingKeyToSkip>=0) {
							afterArgs = new Object[args.length-1];
							for(int i=0,j=0;i<args.length;i++) {
								if(i!=indexOfShardingKeyToSkip) {
									afterArgs[j] = args[i];
									j++;
								}
							}
							Object shardingArgObj = args[indexOfShardingKeyToSkip];
							shardingKey = Integer.parseInt(shardingArgObj.toString());
							if(snowFlake)
								shardingKey = SnowFlake.extractShardingKey(shardingKey);
						}
						return executeUpdate(executeUpdateAnnotation.sql(), executeUpdateAnnotation.masterOrSlave(), executeUpdateAnnotation.anyway(), shardingKey, start, afterArgs);
					}
					return null;
				}
			});
			/*try {
				Object[] args = null;
				executeUpdate("", MasterOrSlave.MASTER, true, args);
				executeUpdate("", MasterOrSlave.MASTER, true, 8192, args);
			} catch (SQLException | InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}*/
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
	public Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, MasterOrSlave masterOrSlave,
			Object... args) throws SQLException, IOException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException,
			InterruptedException, ExecutionException {
		return this.executeQuery(sql, returnType, returnItemType, masterOrSlave, false, args);
	}

	@Override
	public Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, MasterOrSlave masterOrSlave, boolean anyway, Object... args)
			throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, InterruptedException, ExecutionException {
		return this.executeQuery(sql, returnType, returnItemType, masterOrSlave, anyway, System.currentTimeMillis(), args);
	}

	private Object executeQuery(String sql, Class<?> returnType, Class<?> returnItemType, MasterOrSlave masterOrSlave, boolean anyway, long start, Object[] args)
			throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, InterruptedException, ExecutionException {
		Map<String, Method> map = classMethodMap.get(returnItemType);
		if(!this.typeSupported(returnItemType))
			Assert.isTrue(map!=null, returnItemType.getName()+" must be marked by @DTO.");
		int shardCount = dataSource.getResolvedDataSources().size()>>1;
		boolean returnDto = returnType.getName().equals(returnItemType.getName());
		List<FutureTask<List<Object>>> tasks = new ArrayList<>(shardCount);
		for(int i=0;i<shardCount;i++) {
			int shardIndex = i;
	        FutureTask<List<Object>> task = new FutureTask<>(new Callable<List<Object>>() {
				@Override
				public List<Object> call() throws Exception {
					List<Object> list =  new ArrayList<>(returnDto?1:10);
					String key = masterOrSlave.value()+shardIndex;
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
						ResultSetMetaData rsmd = rs.getMetaData();
						int columnCount = rsmd.getColumnCount();
						while(rs.next()) {
							Object item = null;
							if(typeSupported(returnType)) {
								item = toObjectByType(rs, 1, returnType);
							} else {
								if(typeSupported(returnItemType)) {
									item = toObjectByType(rs, 1, returnItemType);
								} else {
									item = returnItemType.getDeclaredConstructor().newInstance();
									for(int i=1;i<=columnCount;i++)
										loadItem(map, item, rsmd, rs, i);
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
						log.info("第{}个分片耗时========Duration============{}", shardIndex, (System.currentTimeMillis()-start));
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
	        if(anyway)
	        	new Thread(task).start();
	        else
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
				Thread.sleep(50);
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

	private boolean typeSupported(Class<?> type) {
		return int.class.isAssignableFrom(type)||Integer.class.isAssignableFrom(type)||
		long.class.isAssignableFrom(type)||Long.class.isAssignableFrom(type)||BigInteger.class.isAssignableFrom(type)||
		String.class.isAssignableFrom(type)||
		BigDecimal.class.isAssignableFrom(type)||
		Date.class.isAssignableFrom(type)||Timestamp.class.isAssignableFrom(type)||
		Time.class.isAssignableFrom(type)||
		float.class.isAssignableFrom(type)||Float.class.isAssignableFrom(type)||
		double.class.isAssignableFrom(type)||Double.class.isAssignableFrom(type)||
		boolean.class.isAssignableFrom(type)||Boolean.class.isAssignableFrom(type)||
		byte.class.isAssignableFrom(type)||Byte.class.isAssignableFrom(type)||
		short.class.isAssignableFrom(type)||Short.class.isAssignableFrom(type)||
		Array.class.isAssignableFrom(type)||
		Blob.class.isAssignableFrom(type)||
		Clob.class.isAssignableFrom(type)||
		byte[].class.isAssignableFrom(type);
	}

	private Object toObjectByType(ResultSet rs, int columnIndex, Class<?> type) throws SQLException, IOException {
		Object v = rs.getObject(columnIndex);
		if(!type.isInstance(v)) {
			if(String.class.isAssignableFrom(type)) {
				v = rs.getString(columnIndex);
			} else if(boolean.class.isAssignableFrom(type)||Boolean.class.isAssignableFrom(type)) {
				v = rs.getBoolean(columnIndex);
			} else if(byte.class.isAssignableFrom(type)||Byte.class.isAssignableFrom(type)) {
				v = rs.getByte(columnIndex);
			} else if(short.class.isAssignableFrom(type)||Short.class.isAssignableFrom(type)) {
				v = rs.getShort(columnIndex);
			} else if(int.class.isAssignableFrom(type)||Integer.class.isAssignableFrom(type)) {
				v = rs.getInt(columnIndex);
			} else if(long.class.isAssignableFrom(type)||Long.class.isAssignableFrom(type)||BigInteger.class.isAssignableFrom(type)) {
				v = rs.getLong(columnIndex);
			} else if(float.class.isAssignableFrom(type)||Float.class.isAssignableFrom(type)) {
				v = rs.getFloat(columnIndex);
			} else if(double.class.isAssignableFrom(type)||Double.class.isAssignableFrom(type)) {
				v = rs.getDouble(columnIndex);
			} else if(BigDecimal.class.isAssignableFrom(type)) {
				v = rs.getBigDecimal(columnIndex);
			} else if(Date.class.isAssignableFrom(type)||Timestamp.class.isAssignableFrom(type)) {
				v = rs.getTimestamp(columnIndex);
			} else if(Time.class.isAssignableFrom(type)) {
				v = rs.getTime(columnIndex);
			} else if(Array.class.isAssignableFrom(type)) {
				v = rs.getArray(columnIndex);
			} else if(Blob.class.isAssignableFrom(type)) {
				v = rs.getBlob(columnIndex);
			} else if(Clob.class.isAssignableFrom(type)) {
				v = rs.getClob(columnIndex);
			} else if(byte[].class.isAssignableFrom(type)) {
				InputStream in = null;
				try {
					in = rs.getBinaryStream(columnIndex);
					byte[] buf = new byte[in.available()];
					in.read(buf);
					v = buf;
				} finally {
					if(in!=null)
						in.close();
				}
			}
		}
		return v;
	}

	private void loadItem(Map<String, Method> map, Object item, ResultSetMetaData rsmd, ResultSet rs, int i) throws SQLException, IOException, IllegalAccessException, InvocationTargetException {
		String columnName = rsmd.getColumnLabel(i);
		Method setterMethod = map.get(columnName);
		if(setterMethod!=null)
			setterMethod.invoke(item, this.toObjectByType(rs, i, setterMethod.getParameterTypes()[0]));
	}

	@Override
	public int[] executeUpdate(String sql, MasterOrSlave masterOrSlave, Object... args)
			throws SQLException, InterruptedException, ExecutionException {
		return this.executeUpdate(sql, masterOrSlave, false, -1, args);
	}

	@Override
	public int[] executeUpdate(String sql, MasterOrSlave masterOrSlave, boolean anyway, long shardingKeyToSkip, Object... args) throws SQLException, InterruptedException, ExecutionException {
		return this.executeUpdate(sql, masterOrSlave, anyway, shardingKeyToSkip, System.currentTimeMillis(), args);
	}

	private int[] executeUpdate(String sql, MasterOrSlave masterOrSlave, boolean anyway, long shardingKeyToSkip, long start, Object[] args) throws SQLException, InterruptedException, ExecutionException {
		int shardCount = dataSource.getResolvedDataSources().size()>>1;
		long shardToSkip = shardingKeyToSkip>=0?shardingKeyToSkip&(shardCount-1):-1;
		List<FutureTask<Integer>> tasks = new ArrayList<>(shardCount);
		for(int i=0;i<shardCount;i++) {
			if(i==shardToSkip)
				continue;
			int shardIndex = i;
	        FutureTask<Integer> task = new FutureTask<>(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					String key = masterOrSlave.value()+shardIndex;
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
						log.info("第{}个分片耗时========Duration============{}", shardIndex, (System.currentTimeMillis()-start));
						if(statment!=null)
							statment.close();
						if(conn!=null)
							conn.close();
					}
				}
	        });
	        tasks.add(task);
	        if(anyway)
	        	new Thread(task).start();
	        else
	        	threadPoolExecutor.submit(task);
		}
		int completedCount = 0;
		boolean[] compleatedTasks = new boolean[tasks.size()];
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
			if(completedCount>=tasks.size())
				break;
			else
				Thread.sleep(50);
		}
		int[] toReturn = new int[shardCount];
		for(int i=0;i<tasks.size();i++) {
			FutureTask<Integer> task = tasks.get(i);
			toReturn[i] = task.get();
		}
		return toReturn;
	}
}