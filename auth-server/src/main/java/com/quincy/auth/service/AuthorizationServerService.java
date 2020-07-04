package com.quincy.auth.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.quincy.auth.o.XSession;
import com.quincy.auth.o.User;

public interface AuthorizationServerService {
//	public XSession setSession(String jsessionid, String originalJsessionid, Long userId, AuthCallback callback) throws IOException, ClassNotFoundException;
	public XSession setSession(HttpServletRequest request, String originalJsessionid, Long userId, AuthCallback callback) throws Exception;
	public void updateSession(User user) throws IOException;
	public void updateSession(List<User> users) throws IOException;
}