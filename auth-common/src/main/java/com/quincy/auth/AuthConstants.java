package com.quincy.auth;

import java.util.Map;

public class AuthConstants {
	public static Map<String, String> PERMISSIONS;//权限英文标识转中文
	public final static String ATTR_DENIED_PERMISSION = "denied_permission";
	public final static String ATTR_SESSION = "xsession";//改了会影响页面模板，要同时改
	public final static String URI_PWD_SET = "/usr/pwd/set";
}