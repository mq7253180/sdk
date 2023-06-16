package com.quincy.auth.service;

import java.io.IOException;
import java.util.List;

import com.quincy.auth.o.XSession;

import jakarta.servlet.http.HttpServletRequest;

import com.quincy.auth.o.User;

public interface AuthorizationServerService {
//	public XSession setSession(String jsessionid, String originalJsessionid, Long userId, AuthCallback callback) throws IOException, ClassNotFoundException;
	public XSession setSession(HttpServletRequest request, AuthCallback callback) throws Exception;
	public void updateSession(User user) throws IOException;
	public void updateSession(List<User> users) throws IOException;
}