package com.quincy.auth.service;

import com.quincy.auth.entity.UserEntity;
import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
import com.quincy.sdk.o.User;

public interface UserService {
	public UserEntity update(UserEntity vo);
	public Long findUserId(String loginName);
	public User find(Long id, Client client);
	public void updatePassword(Long userId, String password);
	public Long add(UserEntity vo);
	public boolean createMapping(String loginName, Long userId);
	public Long createMapping(String loginName);
	public Result updateMapping(String oldLoginName, String newLoginName, UserUpdation userUpdation);
	public void deleteMappingAndUpdateUser(String oldLoginName, UserUpdation userUpdation, Long userId);
}