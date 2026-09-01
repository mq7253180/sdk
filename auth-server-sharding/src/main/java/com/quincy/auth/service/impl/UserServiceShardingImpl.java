package com.quincy.auth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.quincy.sdk.Result;
import com.quincy.auth.entity.UserDto;
import com.quincy.auth.service.UserService;
import com.quincy.auth.service.UserServiceShardingProxy;
import com.quincy.auth.service.UserUpdation;

@Primary
@Service
public class UserServiceShardingImpl extends UserServiceImpl implements UserService {
	@Autowired
	private UserServiceShardingProxy userServiceShardingProxy;

	@Override
	public Long add(UserDto vo) {
		Long userId = vo.getId();
		Assert.notNull(userId, "必须先通过SnowFlake.nextId()生成userId！");
		this.userServiceShardingProxy.add(userId, vo);
		return vo.getId();
	}

	@Override
	public Result updateMapping(String oldLoginName, String newLoginName, UserUpdation userUpdation) {
		Long userId = this.userServiceShardingProxy.findUserId(oldLoginName.hashCode(), oldLoginName);
		Assert.notNull(userId, "开发错误：旧手机号、邮箱、用户名不存在，请检查！");
		if(this.userServiceShardingProxy.createMapping(newLoginName.hashCode(), newLoginName)==null)
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
		userServiceShardingProxy.update(userId, vo);
	}
}