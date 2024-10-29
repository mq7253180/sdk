package com.quincy.auth.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quincy.auth.o.UserDto;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.annotation.sharding.ShardingKey;

@Component
public class UserDaoProxy {
	@Autowired
	private UserDao userDao;

	@ReadOnly(reRoute = true)
	public UserDto find(@ShardingKey long shardingKey, Long id) {
		return userDao.find(id);
	}
}
