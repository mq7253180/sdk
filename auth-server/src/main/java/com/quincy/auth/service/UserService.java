package com.quincy.auth.service;

import com.quincy.auth.entity.UserEntity;
import com.quincy.sdk.Client;
import com.quincy.sdk.o.User;

public interface UserService {
	public UserEntity update(UserEntity vo);
	public User find(String loginName, Client client);
	public User find(Long id, Client client);
	public void updatePassword(Long userId, String password);
}