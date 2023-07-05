package com.quincy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Set;

import javax.sql.DataSource;

import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;

import com.quincy.core.db.RoutingDataSource;
import com.quincy.sdk.annotation.AllShardSQL;
import com.quincy.sdk.annotation.Select;

@Configuration
public class AllShardingConfiguration implements BeanDefinitionRegistryPostProcessor {
	@Autowired
	private DataSource dataSource;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Reflections f = new Reflections("");
		Set<Class<?>> set = f.getTypesAnnotatedWith(AllShardSQL.class);
		for(Class<?> clazz:set) {
			Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {clazz}, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					long start = System.currentTimeMillis();
					Select annotation = method.getAnnotation(Select.class);
					if(annotation!=null) {
						String sql = annotation.value();
						RoutingDataSource realDataSource = (RoutingDataSource)dataSource;
						int shardCount = realDataSource.getResolvedDataSources().size()/2;
						System.out.println("Duration1===================="+(System.currentTimeMillis()-start));
						for(int i=0;i<shardCount;i++) {
							String key = annotation.masterOrSlave().value()+i;
							Connection conn = realDataSource.getResolvedDataSources().get(key).getConnection();
							PreparedStatement statment = conn.prepareStatement(sql);
//							statment.setString(0, sql);
							ResultSet rs = statment.executeQuery();
							while(rs.next()) {
								ResultSetMetaData rsmd = rs.getMetaData();
								int columnCount = rsmd.getColumnCount();
								StringBuilder sb = new StringBuilder();
								for(int j=1;j<=columnCount;j++) {
									sb.append(rsmd.getColumnName(j));
									sb.append("---");
									sb.append(rsmd.getColumnType(j));
									sb.append("---");
									sb.append(rs.getString(j));
									sb.append("；");
								}
								System.out.println("分片"+i+": "+sb);
							}
							conn.close();
						}
						System.out.println("Duration2===================="+(System.currentTimeMillis()-start));
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
}