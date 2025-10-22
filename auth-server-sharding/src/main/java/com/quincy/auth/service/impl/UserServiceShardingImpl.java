package com.quincy.auth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
import com.quincy.sdk.SnowFlake;
import com.quincy.sdk.o.User;
import com.quincy.auth.entity.UserDto;
import com.quincy.auth.service.UserService;
import com.quincy.auth.service.UserServiceShardingProxy;
import com.quincy.auth.service.UserUpdation;

@Primary
@Service
public class UserServiceShardingImpl implements UserService {
	@Autowired
	private UserServiceShardingProxy userServiceShardingProxy;

	@Override
	public void loadAuth(User user) {
		userServiceShardingProxy.loadAuth(SnowFlake.extractShardingKey(user.getId()), user);
	}

	@Override
	public UserDto update(UserDto vo) {
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
		this.userServiceShardingProxy.updatePassword(userId, password);
	}

	@Override
	public Long add(UserDto vo) {
		Long userId = vo.getId();
		Assert.notNull(userId, "必须先通过SnowFlake.nextId()生成userId！");
		Long shardingKey = SnowFlake.extractShardingKey(userId);
		this.userServiceShardingProxy.add(shardingKey, vo);
		return vo.getId();
	}

	@Override
	public boolean createMapping(String loginName, Long userId) {
		return this.userServiceShardingProxy.createMapping(loginName.hashCode(), loginName, userId);
	}

	@Override
	public Long createMapping(String loginName) {
		Long userId = SnowFlake.nextId();
		return this.userServiceShardingProxy.createMapping(loginName.hashCode(), loginName, userId)?userId:null;
	}

	@Override
	public Result updateMapping(String oldLoginName, String newLoginName, UserUpdation userUpdation) {
		Long userId = this.userServiceShardingProxy.findUserId(oldLoginName.hashCode(), oldLoginName);
		Assert.notNull(userId, "开发错误：旧手机号、邮箱、用户名不存在，请检查！");
		if(!this.userServiceShardingProxy.createMapping(newLoginName.hashCode(), newLoginName, userId))
			return new Result(0, "auth.mapping.new");
		this.deleteMappingAndUpdateUser(oldLoginName, userUpdation, userId);
		return new Result(1, "status.success");
	}

	@Override
	public void deleteMappingAndUpdateUser(String oldLoginName, UserUpdation userUpdation, Long userId) {
		this.userServiceShardingProxy.deleteMapping(oldLoginName.hashCode(), oldLoginName);
		UserDto vo = new UserDto();
		vo.setId(userId);
		userUpdation.setLoginName(vo);
		userServiceShardingProxy.update(SnowFlake.extractShardingKey(userId), vo);
	}

	@Override
	public int updateJsessionidPcBrowser(Long id, String jsessionid) {
		return this.userServiceShardingProxy.updateJsessionidPcBrowser(id, jsessionid);
	}

	@Override
	public int updateJsessionidMobileBrowser(Long id, String jsessionid) {
		return this.userServiceShardingProxy.updateJsessionidMobileBrowser(id, jsessionid);
	}

	@Override
	public int updateJsessionidApp(Long id, String jsessionid) {
		return this.userServiceShardingProxy.updateJsessionidApp(id, jsessionid);
	}
}