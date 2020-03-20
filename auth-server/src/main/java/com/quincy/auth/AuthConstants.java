package com.quincy.auth;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

public class AuthConstants {
	public static Map<String, String> PERMISSIONS;
	public final static Map<String, HttpSession> SESSIONS = new HashMap<String, HttpSession>(1000);
	public final static String PARAM_REDIRECT_TO = "redirectTo";
	public final static String URI_INDEX = "/index";
	public final static int LOGIN_STATUS_PWD_INCORRECT = -3;
}