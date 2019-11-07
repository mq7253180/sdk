package com.quincy.auth;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

public class AuthConstants {
	public static Map<String, String> PERMISSIONS;
	public final static Map<String, HttpSession> SESSIONS = new HashMap<String, HttpSession>(1000);
	public final static String PACKAGE_NAME_ENTITY = "com.quincy.auth.entity";
	public final static String PACKAGE_NAME_REPOSITORY = "com.quincy.auth.dao";
	public final static String PACKAGE_NAME_MAPPER = "com.quincy.auth.mapper";
}
