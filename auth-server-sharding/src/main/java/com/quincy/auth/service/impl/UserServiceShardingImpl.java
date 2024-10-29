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
	/**
	 * 通过手机号、邮箱、用户名字符串的hashCode一次路由查user_id映射关系表，得到user_id
	 * 通过从user_id中提取到的shardingKey二次路由查库，查到用户信息
	 */
	@Override
	public User find(String loginName, Client client) {
		return this.userServiceShardingProxy.find(loginName.hashCode(), loginName, client);
	}

	@Override
	public User find(Long id, Client client) {
		return this.userServiceShardingProxy.find(SnowFlake.extractShardingKey(id), id, client);
	}

	@Override
	public void updatePassword(Long userId, String password) {
		this.userServiceShardingProxy.updatePassword(SnowFlake.extractShardingKey(userId), userId, password);
	}
	
}