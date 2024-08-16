package com.quincy.auth.service;

import com.quincy.auth.o.User;
import com.quincy.auth.o.XSession;
import com.quincy.sdk.annotation.sharding.ShardingKey;

public interface ShardingXSessionService {
	public XSession create(@ShardingKey Integer shardingKey, User user);
}