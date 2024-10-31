package com.quincy.auth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.quincy.sdk.Client;
import com.quincy.sdk.SnowFlake;
import com.quincy.sdk.o.User;
import com.quincy.auth.entity.UserEntity;
import com.quincy.auth.service.UserService;
import com.quincy.auth.service.UserServiceShardingProxy;

@Primary
@Service
public class UserServiceShardingImpl implements UserService {
	@Autowired
	private UserServiceShardingProxy userServiceShardingProxy;

	@Override
	public UserEntity update(UserEntity vo) {
		return this.userServiceShardingProxy.update(SnowFlake.extractShardingKey(vo.getId()), vo);
	}

	@Override
	public Long findUserId(String loginName) {
		return this.userServiceShardingProxy.findUserId(loginName.hashCode(), loginName);
	}

	@Override
	public User find(Long id, Client client) {
		return this.userServiceShardingProxy.find(SnowFlake.extractShardingKey(id), id, client);
	}

	@Override
	public void updatePassword(Long userId, String password) {
		this.userServiceShardingProxy.updatePassword(SnowFlake.extractShardingKey(userId), userId, password);
	}

	@Override
	public void add(UserEntity vo) {
		Long shardingKey = SnowFlake.extractShardingKey(vo.getId());
		this.userServiceShardingProxy.add(shardingKey, vo);
	}

	@Override
	public void createMapping(String loginName, Long userId) {
		this.userServiceShardingProxy.createMapping(loginName.hashCode(), loginName, userId);
	}

	@Override
	public Long createMapping(String loginName) {
		Long userId = SnowFlake.nextId();
		this.userServiceShardingProxy.createMapping(loginName.hashCode(), loginName, userId);
		return userId;
	}
}