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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class JdbcDaoConfiguration implements BeanDefinitionRegistryPostProcessor, JdbcDao {
	private DataSource dataSource;
	private Map<Class<?>, Map<String, Method>> classMethodMap;
	private static Map<String, String> selectionSqlCache = new HashMap<String, String>();

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
					Assert.isTrue(returnType.getName().equals(int.class.getName())||returnType.getName().equals(Integer.class.getName()), "Return type must be int[] or Integer[].");
					if(executeUpdateAnnotation!=null) {
						String sql = executeUpdateAnnotation.sql();
						int rowCount = -1;
						if(!executeUpdateAnnotation.withHistory()) {
							rowCount = executeUpdate(sql, args);
						} else {
							String selectionSql = CommonHelper.trim(executeUpdateAnnotation.selectionSql());
							rowCount = selectionSql==null?executeUpdateWithHistory(sql, args):executeUpdateWithHistory(sql, selectionSql, args);
						}
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
				for(int i=0;i<args.length;i++)
					statment.setObject(i+1, args[i]);
			}
			rs = statment.executeQuery();
			while(rs.next()) {
				Object item = returnItemType.getDeclaredConstructor().newInstance();
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				for(int i=1;i<=columnCount;i++) {
					String columnName = rsmd.getColumnLabel(i);
					Method setterMethod = map.get(columnName);
					if(setterMethod!=null) {
						Object v = rs.getObject(i);
						Class<?> parameterType = setterMethod.getParameterTypes()[0];
						if(!parameterType.isInstance(v)) {
							if(parameterType.getName().equals(String.class.getName())) {
								v = rs.getString(i);
							} else if(parameterType.getName().equals(boolean.class.getName())||parameterType.getName().equals(Boolean.class.getName())) {
								v = rs.getBoolean(i);
							} else if(parameterType.getName().equals(byte.class.getName())||parameterType.getName().equals(Byte.class.getName())) {
								v = rs.getByte(i);
							} else if(parameterType.getName().equals(short.class.getName())||parameterType.getName().equals(Short.class.getName())) {
								v = rs.getShort(i);
							} else if(parameterType.getName().equals(int.class.getName())||parameterType.getName().equals(Integer.class.getName())) {
								v = rs.getInt(i);
							} else if(parameterType.getName().equals(long.class.getName())||parameterType.getName().equals(Long.class.getName())) {
								v = rs.getLong(i);
							} else if(parameterType.getName().equals(float.class.getName())||parameterType.getName().equals(Float.class.getName())) {
								v = rs.getFloat(i);
							} else if(parameterType.getName().equals(double.class.getName())||parameterType.getName().equals(Double.class.getName())) {
								v = rs.getDouble(i);
							} else if(parameterType.getName().equals(BigDecimal.class.getName())) {
								v = rs.getBigDecimal(i);
							} else if(parameterType.getName().equals(Date.class.getName())||parameterType.getName().equals(java.sql.Date.class.getName())) {
								v = rs.getDate(i);
							} else if(parameterType.getName().equals(Time.class.getName())) {
								v = rs.getTime(i);
							} else if(parameterType.getName().equals(Timestamp.class.getName())) {
								v = rs.getTimestamp(i);
							} else if(parameterType.getName().equals(Array.class.getName())) {
								v = rs.getArray(i);
							} else if(parameterType.getName().equals(Blob.class.getName())) {
								v = rs.getBlob(i);
							} else if(parameterType.getName().equals(Clob.class.getName())) {
								v = rs.getClob(i);
							} else if(parameterType.getName().equals(byte[].class.getName())) {
								InputStream in = null;
								try {
									in = rs.getBinaryStream(i);
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
				for(int i=0;i<args.length;i++)
					statment.setObject(i+1, args[i]);
			}
			return statment.executeUpdate();
		} finally {
			if(statment!=null)
				statment.close();
			if(connectionHolder==null&&conn!=null)
				conn.close();
		}
	}

	@Override
	public int executeUpdateWithHistory(String sql, Object... args) throws SQLException {
		String selectSql = selectionSqlCache.get(sql);
		if(selectSql==null) {
			synchronized(selectionSqlCache) {
				selectSql = selectionSqlCache.get(sql);
				if(selectSql==null) {
					selectSql = sql.replaceFirst("update", "SELECT {0} FROM").replaceFirst("UPDATE", "SELECT {0} FROM").replaceFirst(" set ", " SET ").replaceFirst(" where ", " WHERE ");
					int setIndexOf = selectSql.indexOf(" SET ");
					int whereIndexOf = selectSql.indexOf(" WHERE ");
					String fields = "id,"+selectSql.substring(setIndexOf+" SET ".length(), whereIndexOf).replaceAll("\s", "").replaceAll("=\\?", "");
					selectSql = selectSql.substring(0, setIndexOf)+selectSql.substring(whereIndexOf);
					selectSql = MessageFormat.format(selectSql, fields);
					selectionSqlCache.put(sql, selectSql);
				}
			}
		}
		return this.executeUpdateWithHistory(sql, selectSql, false, args);
	}

	@Override
	public int executeUpdateWithHistory(String sql, String selectSql, Object... args)
			throws SQLException {
		return this.executeUpdateWithHistory(sql, selectSql, true, args);
	}

	private int executeUpdateWithHistory(String sql, String selectSql, boolean valueFuctionalized, Object... args) throws SQLException {
		boolean selfConn = true;
		Connection conn = null;
		Object connectionHolder = TransactionSynchronizationManager.getResource(dataSource);
		if(connectionHolder==null) {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
		} else {
			conn = ((ConnectionHolder)connectionHolder).getConnection();
			selfConn = false;
		}
		PreparedStatement statment = null;
		PreparedStatement selectStatment = null;
		PreparedStatement updationAutoIncrementStatment = null;
		PreparedStatement updationStatment = null;
		PreparedStatement updationFieldStatment = null;
		ResultSet autoIncrementRs = null;
		ResultSet oldValueRs = null;
		ResultSet newValueRs = null;
		Map<String, DataIdMeta> dataIdMetaData = new HashMap<String, DataIdMeta>();
		try {
			selectStatment = conn.prepareStatement(selectSql);
			ResultSetMetaData rsmd = selectStatment.getMetaData();
			Map<String, Map<Object, Map<String, String>>> oldValueTables = new HashMap<String, Map<Object, Map<String, String>>>();
			int columnCount = rsmd.getColumnCount();
			updationAutoIncrementStatment = conn.prepareStatement("SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE table_schema=? AND table_name=?;");
			updationAutoIncrementStatment.setString(1, conn.getCatalog());
			updationAutoIncrementStatment.setString(2, "s_updation");
			autoIncrementRs = updationAutoIncrementStatment.executeQuery();
			autoIncrementRs.next();
			Long updationId = autoIncrementRs.getLong("AUTO_INCREMENT");
			autoIncrementRs.close();
			for(int i=1;i<=columnCount;i++) {
				String columnName = rsmd.getColumnName(i);
				String tableName = rsmd.getTableName(i);
				if(columnName.equals("id"))
					dataIdMetaData.put(tableName, new DataIdMeta(i, rsmd.getColumnClassName(i), conn, updationId));
				if(oldValueTables.get(tableName)==null)
					oldValueTables.put(tableName, new HashMap<Object, Map<String, String>>());
			}
			int questionMarkCount = symolCount(selectSql, '?');
			int offset = args.length-questionMarkCount-1;
			statment = conn.prepareStatement(sql);
			if(args!=null&&args.length>0) {
				for(int i=1;i<=questionMarkCount;i++)
					selectStatment.setObject(i, args[offset+i]);
				for(int i=0;i<args.length;i++)
					statment.setObject(i+1, args[i]);
			}
			oldValueRs = selectStatment.executeQuery();
			while(oldValueRs.next()) {//将变更前的旧值放进一个三维的Map里，分别以表名、id、字段名作为key
				for(int i=1;i<=columnCount;i++) {
					String columnName = rsmd.getColumnName(i);
					if(columnName.equals("id"))
						continue;
					String tableName = rsmd.getTableName(i);
					Map<Object, Map<String, String>> table = oldValueTables.get(tableName);
					String dataId = oldValueRs.getString(dataIdMetaData.get(tableName).getColumnIndex());
					Map<String, String> row = table.get(dataId);
					if(row==null) {
						row = new HashMap<String, String>();
						table.put(dataId, row);
					}
					row.put(columnName, oldValueRs.getString(i));
				}
			}
			oldValueRs.close();
			int effected = statment.executeUpdate();
			//如果更新值是函数
			Map<String, String> newValueHolder = null;
			if(valueFuctionalized) {
				newValueHolder = new HashMap<String, String>();
				newValueRs = selectStatment.executeQuery();//查询新值
				while(newValueRs.next()) {//将变更后的新值保存进一维Map中，使用表名、id、字段名组合作为key，以便在遍历旧值三维Map时组合key查询对应的新值
					for(int i=1;i<=columnCount;i++) {
						String columnName = rsmd.getColumnName(i);
						if(columnName.equals("id"))
							continue;
						String tableName = rsmd.getTableName(i);
						String dataId = newValueRs.getString(dataIdMetaData.get(tableName).getColumnIndex());
						newValueHolder.put(tableName+"_"+dataId+"_"+columnName, newValueRs.getString(i));
					}
				}
			}

			updationAutoIncrementStatment.setString(2, "s_updation_row");//s_updation表的自增只取一次，后续s_updation_row表的自增要取多次
			//插入s_updation表，记录sql、参数、时间
			StringBuilder sb = new StringBuilder();
			for(Object param:args)
				sb.append(",").append(param);
			updationStatment = conn.prepareStatement("INSERT INTO s_updation VALUES(?, ?, ?, ?)");
			updationStatment.setLong(1, updationId);
			updationStatment.setString(2, sql);
			updationStatment.setString(3, sb.substring(1));
			updationStatment.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
			updationStatment.executeUpdate();

			updationFieldStatment = conn.prepareStatement("INSERT INTO s_updation_field(p_id, name, old_value, new_value) VALUES(?, ?, ?, ?);");
			for(String tableName:oldValueTables.keySet()) {
				Map<Object, Map<String, String>> rows = oldValueTables.get(tableName);
				PreparedStatement updationRowStatment = dataIdMetaData.get(tableName).getStatement();
				updationRowStatment.setString(3, tableName);
				for(Entry<Object, Map<String, String>> row:rows.entrySet()) {
					autoIncrementRs = updationAutoIncrementStatment.executeQuery();
					autoIncrementRs.next();
					Long updationRowId = autoIncrementRs.getLong("AUTO_INCREMENT");
					autoIncrementRs.close();
					Object dataId = row.getKey();
					updationRowStatment.setLong(1, updationRowId);
					updationRowStatment.setObject(4, dataId);
					updationRowStatment.executeUpdate();
					updationFieldStatment.setLong(1, updationRowId);//设置s_updation_field表的p_id字段值
					if(valueFuctionalized) {
						for(Entry<String, String> field:row.getValue().entrySet()) {
							String columnName = field.getKey();
							updationFieldStatment.setString(2, columnName);
							updationFieldStatment.setString(3, field.getValue());
							updationFieldStatment.setString(4, newValueHolder.get(tableName+"_"+dataId.toString()+"_"+columnName));
							updationFieldStatment.executeUpdate();
						}
					} else {
						for(int i=1;i<=columnCount;i++) {
							String columnName = rsmd.getColumnName(i);
							if(columnName.equals("id"))
								continue;
							updationFieldStatment.setString(2, columnName);
							updationFieldStatment.setString(3, rows.get(dataId).get(columnName));
							updationFieldStatment.setString(4, args[i-2].toString());
							updationFieldStatment.executeUpdate();
						}
					}
				}
			}
			if(selfConn)
				conn.commit();
			return effected;
		} catch(SQLException e) {
			if(selfConn)
				conn.rollback();
			throw e;
		} finally {
			if(oldValueRs!=null)
				oldValueRs.close();
			if(newValueRs!=null)
				newValueRs.close();
			if(autoIncrementRs!=null)
				autoIncrementRs.close();
			if(statment!=null)
				statment.close();
			if(selectStatment!=null)
				selectStatment.close();
			if(updationAutoIncrementStatment!=null)
				updationAutoIncrementStatment.close();
			if(updationStatment!=null)
				updationStatment.close();
			for(DataIdMeta dataIdMeta:dataIdMetaData.values())
				dataIdMeta.getStatement().close();
			if(updationFieldStatment!=null)
				updationFieldStatment.close();
			if(selfConn&&conn!=null)
				conn.close();
		}
	}

	private static int symolCount(String s, char symol) {
		char[] cc = s.toCharArray();
		int count = 0;
		for(char c:cc) {
			if(c==symol)
				count++;
		}
		return count;
	}

	private class DataIdMeta {
		private Integer columnIndex;
		private PreparedStatement statement;

		public DataIdMeta(Integer columnIndex, String className, Connection conn, Long updationId) throws SQLException {
			this.columnIndex = columnIndex;
			String fieldName = className.equals(Integer.class.getName())?"data_id_int":"data_id_str";
			statement = conn.prepareStatement("INSERT INTO s_updation_row(id, p_id, table_name, "+fieldName+") VALUES(?, ?, ?, ?);");
			statement.setLong(2, updationId);
		}
		public Integer getColumnIndex() {
			return columnIndex;
		}
		public PreparedStatement getStatement() {
			return statement;
		}
	}
}