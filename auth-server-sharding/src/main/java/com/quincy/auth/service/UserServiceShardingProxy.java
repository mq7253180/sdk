package com.quincy.auth.service;

import com.quincy.auth.entity.UserEntity;
import com.quincy.sdk.Client;
import com.quincy.sdk.annotation.sharding.ShardingKey;
import com.quincy.sdk.o.User;

public interface UserServiceShardingProxy {
	public UserEntity update(@ShardingKey long shardingKey, UserEntity vo);
	public Long findUserId(@ShardingKey long shardingKey, String loginName);
	public User find(@ShardingKey long shardingKey, Long id, Client client);
	public void updatePassword(@ShardingKey long shardingKey, Long userId, String password);
}