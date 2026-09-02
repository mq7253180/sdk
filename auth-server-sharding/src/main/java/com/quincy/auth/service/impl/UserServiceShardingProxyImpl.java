package com.quincy.auth.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.quincy.auth.entity.UserDto;
import com.quincy.auth.service.UserServiceShardingProxy;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.annotation.jdbc.ShardingKey;

@Service
public class UserServiceShardingProxyImpl extends UserServiceImpl implements UserServiceShardingProxy {
	@Override
	@ReadOnly
	public Long findUserId(@ShardingKey long loginNameHashCode, String loginName) {
		return this.findUserId(loginName);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void add(@ShardingKey(snowFlake = true)long userId, UserDto vo) {
		this.userDao.save(vo.getId(), vo.getUsername(), vo.getName(), vo.getGender(), vo.getPassword(), vo.getMobilePhone(), vo.getEmail(), vo.getAvatar());
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Long createMapping(@ShardingKey long loginNameHashCode, String loginName) {
		return this.createMapping(loginName);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public int deleteMapping(@ShardingKey(snowFlake = true)long loginNameHashCode, String loginName) {
		return this.loginUserMappingDao.deleteByLoginName(loginName);
	}
}