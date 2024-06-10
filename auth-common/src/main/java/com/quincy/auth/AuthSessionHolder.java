package com.quincy.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpSession;

public class AuthSessionHolder {
	public final static Map<String, HttpSession> SESSIONS = new ConcurrentHashMap<String, HttpSession>(1024);
}