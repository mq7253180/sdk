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
		Long userId = SnowFlake.nextId();
		Long shardingKey = SnowFlake.extractShardingKey(userId);
		if(vo.getMobilePhone()!=null)
			this.userServiceShardingProxy.createMapping(vo.getMobilePhone().hashCode(), userId, vo.getMobilePhone());
		if(vo.getEmail()!=null)
			this.userServiceShardingProxy.createMapping(vo.getEmail().hashCode(), userId, vo.getEmail());
		if(vo.getUsername()!=null)
			this.userServiceShardingProxy.createMapping(vo.getUsername().hashCode(), userId, vo.getUsername());
		vo.setId(userId);
		this.userServiceShardingProxy.add(shardingKey, vo);
	}
}