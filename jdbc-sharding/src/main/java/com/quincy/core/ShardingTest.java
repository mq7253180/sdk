package com.quincy.core;

public class ShardingTest {
	public static void main(String[] args) {
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
	}
}