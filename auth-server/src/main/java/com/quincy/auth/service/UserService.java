package com.quincy.auth.service;

import com.quincy.auth.entity.UserEntity;
import com.quincy.sdk.Client;
import com.quincy.sdk.o.User;

public interface UserService {
	public UserEntity update(UserEntity vo);
	public Long findUserId(String loginName);
	public User find(Long id, Client client);
	public void updatePassword(Long userId, String password);
	public void add(UserEntity vo);
	public void createMapping(String loginName, Long userId);
	public Long createMapping(String loginName);
}