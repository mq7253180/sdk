package com.quincy.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.cj.jdbc.Driver;
import com.quincy.core.jdbc.OriginalDriverClassHolder;
import com.quincy.core.jdbc.TbShardingConnection;
import com.quincy.core.jdbc.TbShardingDriver;

public class ShardingTest {
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
/*
//		String phone = "18411055336";
//		String email = "mq7253180@126.com";
//		String idcard = "220203198307253019";
		String username = "mq7253180";
		String username1 = "mqsmart";
		String username2 = "maqiang";
		int patitions = 8;
		int mPatitions = patitions*4;
		String salt = "a";
//		System.out.println((phone+salt).hashCode()%mPatitions);
//		System.out.println((email+salt).hashCode()%mPatitions);
//		System.out.println((idcard+salt).hashCode()%mPatitions);
//		System.out.println(1029%patitions+"---------"+1029%(patitions*4));
		System.out.println((username).hashCode()%patitions);
		System.out.println((username).hashCode()%mPatitions);
		System.out.println((username+salt).hashCode()%mPatitions);
		System.out.println((salt+username).hashCode()%mPatitions);
		System.out.println("-----------------");
		System.out.println((username1).hashCode()%patitions);
		System.out.println((username1).hashCode()%mPatitions);
		System.out.println((username1+salt).hashCode()%mPatitions);
		System.out.println((salt+username1).hashCode()%mPatitions);
		System.out.println("-----------------");
		System.out.println((username2).hashCode()%patitions);
		System.out.println((username2).hashCode()%mPatitions);
		System.out.println((username2+salt).hashCode()%mPatitions);
		System.out.println((salt+username2).hashCode()%mPatitions);
		System.out.println("-----------------");
*/
		String url = "jdbc:mysql://192.168.8.33:3306/ducati?zeroDateTimeBehavior=convertToNull&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8";

		OriginalDriverClassHolder.set(Driver.class);
		java.sql.Driver driver = DriverManager.getDriver(TbShardingDriver.URL_PREFIX+url);
		System.out.println(driver.getClass().getName());
/*
//		Class.forName("com.quincy.core.jdbc.TbShardingDriver");
		TbShardingConnection.setTableNames(new String[] {"b_region"});
		Connection conn = null;
		PreparedStatement stat = null;
		ResultSet rs = null;
		try {
			TbShardingConnection.setShard(0);
			conn = DriverManager.getConnection(TbShardingDriver.URL_PREFIX+url, "admin", "1qazXSW@3edc");
			stat = conn.prepareStatement("SELECT * FROM b_region");
			rs = stat.executeQuery();
			while(rs.next()) {
				System.out.println(rs.getString("cn_name"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(rs!=null) {
				rs.close();
			}
			if(stat!=null) {
				stat.close();
			}
			if(conn!=null) {
				conn.close();
			}
		}
*/
	}
}