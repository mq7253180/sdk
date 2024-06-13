package com.quincy.auth.service;

import com.quincy.auth.o.XSession;

public interface AuthorizationServerService {
//	public XSession setSession(String jsessionid, String originalJsessionid, Long userId, AuthCallback callback) throws IOException, ClassNotFoundException;
	public XSession createXSession(Long userId);
//	public void updateSession(User user) throws IOException;
//	public void updateSession(List<User> users) throws IOException;
}