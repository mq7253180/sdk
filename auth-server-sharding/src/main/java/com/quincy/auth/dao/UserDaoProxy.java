package com.quincy.auth.dao;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quincy.auth.entity.UserEntity;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.annotation.sharding.ShardingKey;

@Component
public class UserDaoProxy {
	@Autowired
	private UserRepository userRepository;

	@ReadOnly
	public UserEntity find(@ShardingKey long shardingKey, Long id) {
		Optional<UserEntity> optional = userRepository.findById(id);
		return optional.isPresent()?optional.get():null;
	}
}
