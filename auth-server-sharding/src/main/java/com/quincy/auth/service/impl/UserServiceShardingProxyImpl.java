package com.quincy.auth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.quincy.auth.dao.LoginUserMappingRepository;
import com.quincy.auth.dao.UserDaoProxy;
import com.quincy.auth.entity.LoginUserMappingEntity;
import com.quincy.auth.entity.UserEntity;
import com.quincy.auth.service.UserServiceShardingProxy;
import com.quincy.sdk.Client;
import com.quincy.sdk.SnowFlake;
import com.quincy.sdk.o.User;

public class UserServiceShardingProxyImpl extends UserServiceImpl implements UserServiceShardingProxy {
	@Autowired
	private LoginUserMappingRepository loginUserMappingRepository;
	@Autowired
	private UserDaoProxy userDaoProxy;

	@Override
	public UserEntity update(long shardingKey, UserEntity vo) {
		return this.update(vo);
	}

	@Override
	public User find(long shardingKey, String loginName, Client client) {
		LoginUserMappingEntity loginUserMappingEntity = loginUserMappingRepository.findByLoginName(loginName);
		if(loginUserMappingEntity==null) {
			return null;
		} else {
			Long userId = loginUserMappingEntity.getUserId();
			UserEntity userEntity = userDaoProxy.find(SnowFlake.extractShardingKey(userId), userId);
			return this.toUser(userEntity, client);
		}
	}

	@Override
	public User find(long shardingKey, Long id, Client client) {
		return this.find(id, client);
	}

	@Override
	public void updatePassword(long shardingKey, Long userId, String password) {
		this.updatePassword(userId, password);
	}
}