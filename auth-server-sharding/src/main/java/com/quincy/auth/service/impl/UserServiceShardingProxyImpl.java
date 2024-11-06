package com.quincy.auth.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.quincy.auth.entity.UserEntity;
import com.quincy.auth.service.UserServiceShardingProxy;
import com.quincy.sdk.Client;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.annotation.sharding.ShardingKey;
import com.quincy.sdk.o.User;

@Service
public class UserServiceShardingProxyImpl extends UserServiceImpl implements UserServiceShardingProxy {
	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public UserEntity update(@ShardingKey long shardingKey, UserEntity vo) {
		return this.update(vo);
	}

	@Override
	@ReadOnly
	public Long findUserId(@ShardingKey long shardingKey, String loginName) {
		return this.findUserId(loginName);
	}

	@Override
	@ReadOnly
	public User find(@ShardingKey long shardingKey, Long id, Client client) {
		User user = this.find(id, client);
		if(user!=null)
			user.setShardingKey(shardingKey);
		return user;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void updatePassword(@ShardingKey long shardingKey, Long userId, String password) {
		this.updatePassword(userId, password);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void add(@ShardingKey long shardingKey, UserEntity vo) {
		this.userRepository.save(vo);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public boolean createMapping(@ShardingKey long shardingKey, String loginName, Long userId) {
		return this.createMapping(loginName, userId);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public int deleteMapping(@ShardingKey long shardingKey, String loginName) {
		return this.loginUserMappingRepository.deleteByLoginName(loginName);
	}
}