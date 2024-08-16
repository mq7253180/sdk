package com.quincy.auth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.quincy.auth.o.User;
import com.quincy.auth.o.XSession;
import com.quincy.auth.service.ShardingXSessionService;
import com.quincy.auth.service.XSessionService;

public class XSessionServiceShardingImpl implements XSessionService {
	@Autowired
	private ShardingXSessionService shardingXSessionService;

	@Override
	public XSession create(User user) {
		return shardingXSessionService.create(user.getShardingKey(), user);
	}
}