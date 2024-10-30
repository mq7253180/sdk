package com.quincy.auth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.quincy.auth.dao.LoginUserMappingRepository;
import com.quincy.auth.dao.UserDaoProxy;
import com.quincy.auth.entity.LoginUserMappingEntity;
import com.quincy.auth.entity.UserEntity;
import com.quincy.auth.o.UserDto;
import com.quincy.auth.service.UserServiceShardingProxy;
import com.quincy.sdk.Client;
import com.quincy.sdk.SnowFlake;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.annotation.sharding.ShardingKey;
import com.quincy.sdk.o.User;

@Service
public class UserServiceShardingProxyImpl extends UserServiceImpl implements UserServiceShardingProxy {
	@Autowired
	private LoginUserMappingRepository loginUserMappingRepository;
	@Autowired
	private UserDaoProxy userDaoProxy;

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public UserEntity update(@ShardingKey long shardingKey, UserEntity vo) {
		return this.update(vo);
	}
	
	@Override
	@ReadOnly
	public User find(@ShardingKey long shardingKey, String loginName, Client client) {
		LoginUserMappingEntity loginUserMappingEntity = loginUserMappingRepository.findByLoginName(loginName);
		if(loginUserMappingEntity==null) {
			return null;
		} else {
			Long userId = loginUserMappingEntity.getUserId();
			long realShardingKey = SnowFlake.extractShardingKey(userId);
			UserDto userDto = userDaoProxy.find(realShardingKey, userId);
			User user = this.toUser(userDto, client);
			user.setShardingKey(realShardingKey);
			return user;
		}
	}

	@Override
	@ReadOnly
	public User find(@ShardingKey long shardingKey, Long id, Client client) {
		return this.find(id, client);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void updatePassword(@ShardingKey long shardingKey, Long userId, String password) {
		this.updatePassword(userId, password);
	}
}