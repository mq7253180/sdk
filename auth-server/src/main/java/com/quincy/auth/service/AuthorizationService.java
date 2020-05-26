package com.quincy.auth.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.quincy.auth.o.DSession;
import com.quincy.auth.o.User;

public interface AuthorizationService {
	public DSession getSession(HttpServletRequest request) throws Exception;
//	public DSession setSession(String jsessionid, String originalJsessionid, Long userId, AuthCallback callback) throws IOException, ClassNotFoundException;
	public DSession setSession(HttpServletRequest request, String originalJsessionid, Long userId, AuthCallback callback) throws Exception;
	public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception;
	public void updateSession(User user) throws IOException;
	public <T extends User> void updateSession(List<T> users) throws IOException;
	public void setExpiry(HttpServletRequest request) throws Exception;
}