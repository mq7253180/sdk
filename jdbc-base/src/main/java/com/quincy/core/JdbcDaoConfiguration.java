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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import com.quincy.core.db.JdbcDaoConstants;
import com.quincy.sdk.DynamicColumn;
import com.quincy.sdk.DynamicField;
import com.quincy.sdk.JdbcDao;
import com.quincy.sdk.annotation.jdbc.ExecuteQuery;
import com.quincy.sdk.annotation.jdbc.ExecuteQueryWIthDynamicColumns;
import com.quincy.sdk.annotation.jdbc.ExecuteUpdate;
import com.quincy.sdk.annotation.jdbc.FindDynamicFields;
import com.quincy.sdk.annotation.jdbc.JDBCDao;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class JdbcDaoConfiguration implements BeanDefinitionRegistryPostProcessor, JdbcDao {
	private DataSource dataSource;
	private Map<Class<?>, Map<String, Method>> classMethodMap;
	private Map<Class<?>, Class<?>> returnTypeMap;
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
					ExecuteQueryWIthDynamicColumns executeQueryWIthDynamicColumnsAnnotation = method.getAnnotation(ExecuteQueryWIthDynamicColumns.class);
					FindDynamicFields findDynamicFieldsAnnotation = method.getAnnotation(FindDynamicFields.class);
					Assert.isTrue(queryAnnotation!=null||executeUpdateAnnotation!=null||executeQueryWIthDynamicColumnsAnnotation!=null||findDynamicFieldsAnnotation!=null, "What do you want to do?");
					Class<?> returnType = method.getReturnType();
					if(queryAnnotation!=null) {
						Class<?> returnItemType = queryAnnotation.returnItemType();
						Assert.isTrue(returnType.getName().equals(List.class.getName())||returnType.getName().equals(ArrayList.class.getName())||returnType.getName().equals(returnItemType.getName()), "Return type must be List or ArrayList or given returnItemType.");
						Object result = executeQuery(queryAnnotation.sql(), queryAnnotation.returnItemType(), returnType, queryAnnotation.newConn(), args);
						log.warn("Duration======{}======{}", queryAnnotation.sql(), (System.currentTimeMillis()-start));
						return result;
					}
					if(executeQueryWIthDynamicColumnsAnnotation!=null) {
						Class<?> returnItemType = executeQueryWIthDynamicColumnsAnnotation.returnItemType();
						Assert.isTrue(returnType.getName().equals(List.class.getName())||returnType.getName().equals(ArrayList.class.getName())||returnType.getName().equals(returnItemType.getName())||classMethodMap.get(returnType)!=null, "Return type must be List or ArrayList or given returnItemType or the type marked by @DynamicColumnQueryDTO.");
						Object result = executeQueryWithDynamicColumns(executeQueryWIthDynamicColumnsAnnotation.sqlFrontHalf(), executeQueryWIthDynamicColumnsAnnotation.tableName(), executeQueryWIthDynamicColumnsAnnotation.returnItemType(), returnType, args);
						log.warn("Duration======{}======{}", executeQueryWIthDynamicColumnsAnnotation.sqlFrontHalf(), (System.currentTimeMillis()-start));
						return result;
					}
					if(executeUpdateAnnotation!=null) {
						Assert.isTrue(returnType.getName().equals(int.class.getName())||returnType.getName().equals(Integer.class.getName()), "Return type must be int or Integer.");
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
					if(findDynamicFieldsAnnotation!=null)
						return findDynamicFields(findDynamicFieldsAnnotation.value());
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
	public void setReturnTypeMap(Map<Class<?>, Class<?>> returnTypeMap) {
		this.returnTypeMap = returnTypeMap;
	}

	@Override
	public Object executeQuery(String sql, Class<?> returnItemType, Class<?> returnType, boolean _newConn, Object... args)
			throws SQLException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		boolean returnDto = returnType.getName().equals(returnItemType.getName());
		List<Object> list = null;
		if(!returnDto)
			list = new ArrayList<>();
		Map<String, Method> map = classMethodMap.get(returnItemType);
		Assert.isTrue(map!=null, returnItemType.getName()+" must be marked by @DTO.");
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Object connectionHolder = TransactionSynchronizationManager.getResource(dataSource);
		boolean newConn = _newConn||connectionHolder==null;
		try {
			conn = newConn?dataSource.getConnection():((ConnectionHolder)connectionHolder).getConnection();
			statement = conn.prepareStatement(sql);
			ResultSetMetaData rsmd = statement.getMetaData();
			int columnCount = rsmd.getColumnCount();
			if(args!=null&&args.length>0) {
				for(int i=0;i<args.length;i++)
					statement.setObject(i+1, args[i]);
			}
			rs = statement.executeQuery();
			while(rs.next()) {
				Object item = returnItemType.getDeclaredConstructor().newInstance();
				for(int i=1;i<=columnCount;i++)
					this.loadItem(map, item, rsmd, rs, i);
				if(returnDto) {
					return item;
				} else
					list.add(item);
			}
			return returnDto?null:list;
		} finally {
			if(rs!=null)
				rs.close();
			if(statement!=null)
				statement.close();
			if(newConn&&conn!=null)
				conn.close();
		}
	}

	private void loadItem(Map<String, Method> map, Object item, ResultSetMetaData rsmd, ResultSet rs, int i) throws SQLException, IOException, IllegalAccessException, InvocationTargetException {
		String columnName = rsmd.getColumnLabel(i);
		Method setterMethod = map.get(columnName);
		if(setterMethod!=null) {
			Class<?> parameterType = setterMethod.getParameterTypes()[0];
			Object v = rs.getObject(i);
			if(!parameterType.isInstance(v)) {
				if(String.class.isAssignableFrom(parameterType)) {
					v = rs.getString(i);
				} else if(boolean.class.isAssignableFrom(parameterType)||Boolean.class.isAssignableFrom(parameterType)) {
					v = rs.getBoolean(i);
				} else if(byte.class.isAssignableFrom(parameterType)||Byte.class.isAssignableFrom(parameterType)) {
					v = rs.getByte(i);
				} else if(short.class.isAssignableFrom(parameterType)||Short.class.isAssignableFrom(parameterType)) {
					v = rs.getShort(i);
				} else if(int.class.isAssignableFrom(parameterType)||Integer.class.isAssignableFrom(parameterType)) {
					v = rs.getInt(i);
				} else if(long.class.isAssignableFrom(parameterType)||Long.class.isAssignableFrom(parameterType)) {
					v = rs.getLong(i);
				} else if(float.class.isAssignableFrom(parameterType)||Float.class.isAssignableFrom(parameterType)) {
					v = rs.getFloat(i);
				} else if(double.class.isAssignableFrom(parameterType)||Double.class.isAssignableFrom(parameterType)) {
					v = rs.getDouble(i);
				} else if(BigDecimal.class.isAssignableFrom(parameterType)) {
					v = rs.getBigDecimal(i);
				} else if(Timestamp.class.isAssignableFrom(parameterType)) {
					v = rs.getTimestamp(i);
				} else if(Time.class.isAssignableFrom(parameterType)) {
					v = rs.getTime(i);
				} else if(Date.class.isAssignableFrom(parameterType)) {
					v = rs.getDate(i);
				} else if(Array.class.isAssignableFrom(parameterType)) {
					v = rs.getArray(i);
				} else if(Blob.class.isAssignableFrom(parameterType)) {
					v = rs.getBlob(i);
				} else if(Clob.class.isAssignableFrom(parameterType)) {
					v = rs.getClob(i);
				} else if(byte[].class.isAssignableFrom(parameterType)) {
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

	@Override
	public int executeUpdate(String sql, Object... args) throws SQLException {
		Connection conn = null;
		PreparedStatement statement = null;
		Object connectionHolder = TransactionSynchronizationManager.getResource(dataSource);
		try {
			conn = connectionHolder==null?dataSource.getConnection():((ConnectionHolder)connectionHolder).getConnection();
			statement = conn.prepareStatement(sql);
			if(args!=null&&args.length>0) {
				for(int i=0;i<args.length;i++)
					statement.setObject(i+1, args[i]);
			}
			return statement.executeUpdate();//只有一步操作，如果是自获取连接无需设置事务，直接自动提交即可，如果是从上下文获取的连接，事务提交还是回滚取决于外层事务
		} finally {
			if(statement!=null)
				statement.close();
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
		Map<String, DataIdMeta> dataIdMetaData = new HashMap<String, DataIdMeta>();
		Map<String, PreparedStatement> updationFieldStatementHolder = new HashMap<String, PreparedStatement>();
		Map<String, Map<Object, Map<String, Object>>> oldValueTables = new HashMap<String, Map<Object, Map<String, Object>>>();
		boolean selfConn = true;
		Connection conn = null;
		PreparedStatement statement = null;
		PreparedStatement selectStatement = null;
		PreparedStatement updationAutoIncrementStatement = null;
		PreparedStatement updationStatement = null;
		ResultSet autoIncrementRs = null;
		ResultSet oldValueRs = null;
		ResultSet newValueRs = null;
		Object connectionHolder = TransactionSynchronizationManager.getResource(dataSource);
		try {
			if(connectionHolder==null) {
				conn = dataSource.getConnection();
				conn.setAutoCommit(false);
				conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			} else {
				conn = ((ConnectionHolder)connectionHolder).getConnection();
				selfConn = false;
			}
			selectStatement = conn.prepareStatement(selectSql);
			ResultSetMetaData rsmd = selectStatement.getMetaData();
			int columnCount = rsmd.getColumnCount();
			updationAutoIncrementStatement = conn.prepareStatement("SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE table_schema=? AND table_name=?;");
			updationAutoIncrementStatement.setString(1, conn.getCatalog());
			updationAutoIncrementStatement.setString(2, "s_updation");
			autoIncrementRs = updationAutoIncrementStatement.executeQuery();
			autoIncrementRs.next();
			Long updationId = autoIncrementRs.getLong("AUTO_INCREMENT");
			autoIncrementRs.close();
			for(int i=1;i<=columnCount;i++) {
				String tableName = rsmd.getTableName(i);
				String columnName = rsmd.getColumnName(i);
				String columnLabel = rsmd.getColumnLabel(i);
				String columnClassName = rsmd.getColumnClassName(i);
				if(columnName.equals("id")||columnLabel.equals("id")) {
					dataIdMetaData.put(tableName, new DataIdMeta(i, columnClassName, conn, updationId));
				} else {
					String valColumnNameSuffix = null;
					if(columnClassName.equals(Integer.class.getName())||columnClassName.equals(Long.class.getName())) {
						valColumnNameSuffix = "int";
					} else if(columnClassName.equals(BigDecimal.class.getName())||columnClassName.equals(Double.class.getName())||columnClassName.equals(Float.class.getName())) {
						valColumnNameSuffix = "decimal";
					} else if(columnClassName.equals(LocalDateTime.class.getName())||columnClassName.equals(Timestamp.class.getName())||columnClassName.equals(java.sql.Date.class.getName())||columnClassName.equals(Time.class.getName())) {
						valColumnNameSuffix = "time";
					} else
						valColumnNameSuffix = "str";
					updationFieldStatementHolder.put(tableName+"_"+columnName, conn.prepareStatement("INSERT INTO s_updation_field(p_id, name, old_value_"+valColumnNameSuffix+", new_value_"+valColumnNameSuffix+") VALUES(?, ?, ?, ?);"));
				}
				if(oldValueTables.get(tableName)==null)
					oldValueTables.put(tableName, new HashMap<Object, Map<String, Object>>());
			}
			int questionMarkCount = symolCount(selectSql, '?');
			int offset = args.length-questionMarkCount-1;
			statement = conn.prepareStatement(sql);
			if(args!=null&&args.length>0) {
				for(int i=1;i<=questionMarkCount;i++)
					selectStatement.setObject(i, args[offset+i]);
				for(int i=0;i<args.length;i++)
					statement.setObject(i+1, args[i]);
			}
			oldValueRs = selectStatement.executeQuery();
			while(oldValueRs.next()) {//将变更前的旧值放进一个三维的Map里，分别以表名、id、字段名作为key
				for(int i=1;i<=columnCount;i++) {
					String columnName = rsmd.getColumnName(i);
					String columnLabel = rsmd.getColumnLabel(i);
					if(columnName.equals("id")||columnLabel.equals("id"))
						continue;
					String tableName = rsmd.getTableName(i);
					Map<Object, Map<String, Object>> table = oldValueTables.get(tableName);
					Object dataId = oldValueRs.getObject(dataIdMetaData.get(tableName).getColumnIndex());
					Map<String, Object> row = table.get(dataId);
					if(row==null) {
						row = new HashMap<String, Object>();
						table.put(dataId, row);
					}
					row.put(columnName, oldValueRs.getObject(i));
				}
			}
			oldValueRs.close();
			int effected = statement.executeUpdate();
			//如果更新值是函数
			Map<String, Object> newValueHolder = null;
			if(valueFuctionalized) {
				newValueHolder = new HashMap<String, Object>();
				newValueRs = selectStatement.executeQuery();//查询新值
				while(newValueRs.next()) {//将变更后的新值保存进一维Map中，使用表名、id、字段名组合作为key，以便在遍历旧值三维Map时组合key查询对应的新值
					for(int i=1;i<=columnCount;i++) {
						String columnName = rsmd.getColumnName(i);
						String columnLabel = rsmd.getColumnLabel(i);
						if(columnName.equals("id")||columnLabel.equals("id"))
							continue;
						String tableName = rsmd.getTableName(i);
						String dataId = newValueRs.getString(dataIdMetaData.get(tableName).getColumnIndex());
						newValueHolder.put(tableName+"_"+dataId+"_"+columnName, newValueRs.getObject(i));
					}
				}
			}
			//插入s_updation表，记录sql、参数、时间
			StringBuilder sb = new StringBuilder();
			for(Object param:args)
				sb.append(",").append(param);
			updationStatement = conn.prepareStatement("INSERT INTO s_updation VALUES(?, ?, ?, ?)");
			updationStatement.setLong(1, updationId);
			updationStatement.setString(2, sql);
			updationStatement.setString(3, sb.substring(1));
			updationStatement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
			updationStatement.executeUpdate();
			//插入s_updation表结束
			updationAutoIncrementStatement.setString(2, "s_updation_row");//s_updation表的自增只取一次，后续s_updation_row表的自增要取多次
			for(String tableName:oldValueTables.keySet()) {
				Map<Object, Map<String, Object>> rows = oldValueTables.get(tableName);
				PreparedStatement updationRowStatement = dataIdMetaData.get(tableName).getStatement();
				updationRowStatement.setString(3, tableName);
				for(Entry<Object, Map<String, Object>> row:rows.entrySet()) {
					autoIncrementRs = updationAutoIncrementStatement.executeQuery();
					autoIncrementRs.next();
					Long updationRowId = autoIncrementRs.getLong("AUTO_INCREMENT");
					autoIncrementRs.close();
					Object dataId = row.getKey();
					updationRowStatement.setLong(1, updationRowId);
					updationRowStatement.setObject(4, dataId);
					updationRowStatement.executeUpdate();
					if(valueFuctionalized) {
						for(Entry<String, Object> field:row.getValue().entrySet()) {
							String columnName = field.getKey();
							PreparedStatement updationFieldStatement = updationFieldStatementHolder.get(tableName+"_"+columnName);
							updationFieldStatement.setLong(1, updationRowId);
							updationFieldStatement.setString(2, columnName);
							updationFieldStatement.setObject(3, field.getValue());
							updationFieldStatement.setObject(4, newValueHolder.get(tableName+"_"+dataId.toString()+"_"+columnName));
							updationFieldStatement.executeUpdate();
						}
					} else {
						for(int i=1;i<=columnCount;i++) {
							String columnName = rsmd.getColumnName(i);
							String columnLabel = rsmd.getColumnLabel(i);
							if(columnName.equals("id")||columnLabel.equals("id"))
								continue;
							PreparedStatement updationFieldStatement = updationFieldStatementHolder.get(tableName+"_"+columnName);
							updationFieldStatement.setLong(1, updationRowId);
							updationFieldStatement.setString(2, columnName);
							updationFieldStatement.setObject(3, rows.get(dataId).get(columnName));
							updationFieldStatement.setObject(4, args[i-2]);
							updationFieldStatement.executeUpdate();
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
			if(statement!=null)
				statement.close();
			if(selectStatement!=null)
				selectStatement.close();
			if(updationAutoIncrementStatement!=null)
				updationAutoIncrementStatement.close();
			if(updationStatement!=null)
				updationStatement.close();
			for(DataIdMeta dataIdMeta:dataIdMetaData.values())
				dataIdMeta.getStatement().close();
			for(PreparedStatement updationFieldStatement:updationFieldStatementHolder.values())
				updationFieldStatement.close();
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
			String dataIdFieldNameSuffix = className.equals(Integer.class.getName())?"int":"str";
			statement = conn.prepareStatement("INSERT INTO s_updation_row(id, p_id, table_name, data_id_"+dataIdFieldNameSuffix+") VALUES(?, ?, ?, ?);");
			statement.setLong(2, updationId);
		}
		public Integer getColumnIndex() {
			return columnIndex;
		}
		public PreparedStatement getStatement() {
			return statement;
		}
	}

	@Override
	public Object executeQueryWithDynamicColumns(String sqlFrontHalf, String tableName, Class<?> returnItemType, Class<?> returnType,
			Object... args) throws SQLException, IOException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, CloneNotSupportedException {
		String returnItemTypeName = returnItemType.getName();
		Map<String, Method> map = classMethodMap.get(returnItemType);
		Assert.isTrue(map!=null, returnItemTypeName+" must be marked by @DTO.");
		Method getterMethod = map.get(JdbcDaoConstants.DYNAMIC_COLUMN_LIST_GETTER_METHOD_KEY);
		Assert.isTrue(getterMethod!=null, "No getter method of the field marked by @DynamicColumns supplied in "+returnItemTypeName+".");
		Method setterMethod = map.get(JdbcDaoConstants.DYNAMIC_COLUMN_LIST_SETTER_METHOD_KEY);
		Assert.isTrue(setterMethod!=null, "No setter method of the field marked by @DynamicColumns supplied in "+returnItemTypeName+".");
		boolean returnWrapper = false;
		boolean returnDto = false;
		Class<?> resultType = null;
		if(returnType.getName().equals(returnItemTypeName)) {
			resultType = returnType;
			returnDto = true;
		} else if(returnType.getName().equals(List.class.getName())||returnType.getName().equals(ArrayList.class.getName())) {
			resultType = returnType;
		} else {
			resultType = this.returnTypeMap.get(returnType);
			Assert.isTrue(resultType!=null, "The return type must be one of List, ArrayList, type marked by @DTO or @DynamicColumnQueryDTO.");
			returnDto = resultType.getName().equals(returnItemTypeName);
			returnWrapper = true;
		}
		List<DynamicColumn> dynamicColumnsModel = new ArrayList<DynamicColumn>();
		List<Object> list = new ArrayList<>();
		String sql = sqlFrontHalf+" s LEFT OUTER JOIN s_dynamic_field_val v ON s.id=v.business_id_str OR s.id=v.business_id_int LEFT OUTER JOIN s_dynamic_field f ON v.field_id=f.id AND f.table_name=?;";
		Map<Object, Object> groupedResultMap = new HashMap<Object, Object>();
		Connection conn = null;
		PreparedStatement dynamicFieldsStatement = null;
		PreparedStatement statement = null;
		ResultSet dynamicFieldsRs = null;
		ResultSet rs = null;
		Object connectionHolder = TransactionSynchronizationManager.getResource(dataSource);
		try {
			conn = connectionHolder==null?dataSource.getConnection():((ConnectionHolder)connectionHolder).getConnection();
			statement = conn.prepareStatement(sql);
			ResultSetMetaData rsmd = statement.getMetaData();
			int columnCount = rsmd.getColumnCount();
			int fieldIdColumnIndex = 0;
			int businessIdColumnIndex = 0;
			String joinConditionToRemove = null;
			for(int i=1;i<=columnCount;i++) {
				String columnName = rsmd.getColumnName(i);
				String columnLabel = rsmd.getColumnLabel(i);
				String tbName = rsmd.getTableName(i);
				if(columnName.equals("id")||columnLabel.equals("id")) {
					if(tbName.equals("s_dynamic_field")) {
						fieldIdColumnIndex = i;
					} else {
						businessIdColumnIndex = i;
						joinConditionToRemove = rsmd.getColumnClassName(i).equals(String.class.getName())?"OR s.id=v.business_id_int ":"s.id=v.business_id_str OR ";
						sql = sql.replaceFirst(joinConditionToRemove, "");
					}
				}
				if(i>2&&fieldIdColumnIndex>0&&businessIdColumnIndex>0)//如果业务数据id和动态字段id都找到后
					break;
			}
			dynamicFieldsStatement = conn.prepareStatement("SELECT * FROM s_dynamic_field WHERE table_name=? ORDER BY sort;");
			dynamicFieldsStatement.setString(1, tableName);
			dynamicFieldsRs = dynamicFieldsStatement.executeQuery();
			while(dynamicFieldsRs.next()) {
				String name = dynamicFieldsRs.getString("name");
				String clazz = dynamicFieldsRs.getString("class");
				dynamicColumnsModel.add(new DynamicColumn(dynamicFieldsRs.getInt("id"), name, clazz, dynamicFieldsRs.getInt("sort")));
			}
			statement.close();
			statement = conn.prepareStatement(sql);
			int tableNameIndex = 1;
			if(args!=null) {
				tableNameIndex = args.length+1;
				if(args.length>0)
					for(int i=0;i<args.length;i++)
						statement.setObject(i+1, args[i]);
			}
			statement.setString(tableNameIndex, tableName);
			rs = statement.executeQuery();
			while(rs.next()) {
				Object businessId = rs.getObject(businessIdColumnIndex);
				Object item = groupedResultMap.get(businessId);
				List<DynamicColumn> dynamicColumns = null;
				if(item==null) {
					item = returnItemType.getDeclaredConstructor().newInstance();
					for(int i=1;i<=columnCount;i++) {
						String tbName = rsmd.getTableName(i);
						if(!tbName.equals("s_dynamic_field")&&!tbName.equals("s_dynamic_field_val"))
							this.loadItem(map, item, rsmd, rs, i);
					}
					dynamicColumns = new ArrayList<DynamicColumn>();
					for(DynamicColumn dynamicColumn:dynamicColumnsModel)
						dynamicColumns.add(dynamicColumn.clone());
					setterMethod.invoke(item, dynamicColumns);
					groupedResultMap.put(businessId, item);
					list.add(item);
				} else
					dynamicColumns = (List)getterMethod.invoke(item);
				Integer id = null;
				Object value = null;
				for(int i=1;i<=columnCount;i++) {
					if(value==null) {
						String tbName = rsmd.getTableName(i);
						String columnName = rsmd.getColumnName(i);
						if(tbName.equals("s_dynamic_field")&&columnName.equals("id")) {
							id = rs.getInt(i);
						} else if(tbName.equals("s_dynamic_field_val")&&columnName.startsWith("value_"))
							value = rs.getObject(i);
					}
				}
				if(value!=null) {
					for(DynamicColumn dynamicColumn:dynamicColumns) {
						if(dynamicColumn.getId().intValue()==id) {
							dynamicColumn.setValue(value);
							break;
						}
					}
				}
			}
			Object itemOrList = null;
			if(returnDto) {
				Object item = null;
				if(list.size()>0) {
					item = list.get(0);
					this.sortDynamicColumns((List)getterMethod.invoke(item));
				}
				itemOrList = item;
			} else {
				for(Object item:list)
					this.sortDynamicColumns((List)getterMethod.invoke(item));
				itemOrList = list;
			}
			return returnWrapper?this.wrap(returnType, dynamicColumnsModel, itemOrList):itemOrList;
		} finally {
			if(dynamicFieldsRs!=null)
				dynamicFieldsRs.close();
			if(rs!=null)
				rs.close();
			if(dynamicFieldsStatement!=null)
				dynamicFieldsStatement.close();
			if(statement!=null)
				statement.close();
			if(connectionHolder==null&&conn!=null)
				conn.close();
		}
	}

	private void sortDynamicColumns(List<DynamicColumn> list) throws IllegalAccessException, InvocationTargetException {
		Collections.sort(list, new Comparator<DynamicColumn>() {
			@Override
			public int compare(DynamicColumn o1, DynamicColumn o2) {
				return o1.getSort()-o2.getSort();
			}
		});
	}

	private Object wrap(Class<?> returnType, List<? extends DynamicField> dynamicFields, Object itemOrList) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Map<String, Method> wrapperDtoMethods = classMethodMap.get(returnType);
		Method fieldsSetterMethod = wrapperDtoMethods.get(JdbcDaoConstants.DYNAMIC_COLUMN_WRAPPER_FIELDS_SETTER_METHOD_KEY);
		Method resultSetterMethod = wrapperDtoMethods.get(JdbcDaoConstants.DYNAMIC_COLUMN_WRAPPER_RESULT_SETTER_METHOD_KEY);
		Assert.isTrue(fieldsSetterMethod!=null&&fieldsSetterMethod!=null, "The setter methods of the fields marked by @DynamicFields or @Result must be supplied in "+returnType.getName()+".");
		Object wrapper = returnType.getDeclaredConstructor().newInstance();
		fieldsSetterMethod.invoke(wrapper, dynamicFields);
		resultSetterMethod.invoke(wrapper, itemOrList);
		return wrapper;
	}

	@Override
	public List<DynamicField> findDynamicFields(String tableName) throws SQLException {
		List<DynamicField> list = new ArrayList<DynamicField>();
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Object connectionHolder = TransactionSynchronizationManager.getResource(dataSource);
		try {
			conn = connectionHolder==null?dataSource.getConnection():((ConnectionHolder)connectionHolder).getConnection();
			statement = conn.prepareStatement("SELECT * FROM s_dynamic_field WHERE table_name=? ORDER BY sort;");
			statement.setString(1, tableName);
			rs = statement.executeQuery();
			while(rs.next()) {
				Integer id = rs.getInt("id");
				String name = rs.getString("name");
				String clazz = rs.getString("class");
				int sort = rs.getInt("sort");
				list.add(new DynamicField(id, name, clazz, sort));
			}
			return list;
		} finally {
			if(rs!=null)
				rs.close();
			if(statement!=null)
				statement.close();
			if(connectionHolder==null&&conn!=null)
				conn.close();
		}
	}
}