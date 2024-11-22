package com.quincy.auth.service;

import com.quincy.auth.entity.UserEntity;
import com.quincy.sdk.Client;
import com.quincy.sdk.o.User;

public interface UserServiceShardingProxy {
	public UserEntity update(long shardingKey, UserEntity vo);
	public Long findUserId(long shardingKey, String loginName);
	public User find(long shardingKey, Long id, Client client);
	public void updatePassword(long shardingKey, Long userId, String password);
	public void add(long shardingKey, UserEntity vo);
	public boolean createMapping(long shardingKey, String loginName, Long userId);
	public int deleteMapping(long shardingKey, String loginName);
}