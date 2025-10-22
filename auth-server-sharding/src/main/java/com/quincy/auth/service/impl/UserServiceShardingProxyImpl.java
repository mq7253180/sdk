package com.quincy.auth.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.quincy.auth.entity.UserDto;
import com.quincy.auth.service.UserServiceShardingProxy;
import com.quincy.sdk.Client;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.annotation.sharding.ShardingKey;
import com.quincy.sdk.o.User;

@Service
public class UserServiceShardingProxyImpl extends UserServiceImpl implements UserServiceShardingProxy {
	@Override
	@ReadOnly
	public void loadAuth(@ShardingKey long shardingKey, User user) {
		this.loadAuth(user);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public UserDto update(@ShardingKey long shardingKey, UserDto vo) {
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
	public void updatePassword(@ShardingKey(snowFlake = true) Long id, String password) {
		this.updatePassword(id, password);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void add(@ShardingKey long shardingKey, UserDto vo) {
		this.userDao.save(vo.getId(), vo.getUsername(), vo.getName(), vo.getGender(), vo.getPassword(), vo.getMobilePhone(), vo.getEmail(), vo.getAvatar());
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public boolean createMapping(@ShardingKey long shardingKey, String loginName, Long userId) {
		return this.createMapping(loginName, userId);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public int deleteMapping(@ShardingKey long shardingKey, String loginName) {
		return this.loginUserMappingDao.deleteByLoginName(loginName);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public int updateJsessionidPcBrowser(@ShardingKey(snowFlake = true) Long id, String jsessionid) {
		return this.userDao.updateJsessionidPcBrowser(jsessionid, id);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public int updateJsessionidMobileBrowser(@ShardingKey(snowFlake = true) Long id, String jsessionid) {
		return this.userDao.updateJsessionidMobileBrowser(jsessionid, id);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public int updateJsessionidApp(@ShardingKey(snowFlake = true) Long id, String jsessionid) {
		return this.userDao.updateJsessionidApp(jsessionid, id);
	}
}