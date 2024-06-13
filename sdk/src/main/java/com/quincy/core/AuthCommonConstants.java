package com.quincy.core;

import java.util.Map;

public class AuthCommonConstants {
	public static Map<String, String> PERMISSIONS;
	public final static int LOGIN_STATUS_PWD_INCORRECT = -3;
	public final static String ATTR_DENIED_PERMISSION = "denied_permission";
	public final static String PARA_NAME_USERNAME = "username";
	public final static String PARA_NAME_PASSWORD = "password";
	public final static String PARA_NAME_VCODE = "vcode";
	public final static String ATTR_KEY_VCODE_PWD_LOGIN = "vcode_pwd_login";
	public final static String ATTR_KEY_VCODE_LOGIN = "vcode_login";
	public final static String ATTR_KEY_USERNAME = PARA_NAME_USERNAME;
	public final static String ATTR_KEY_VCODE_ORIGINAL_MXA_INACTIVE_INTERVAL = "vcode";
}