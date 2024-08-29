package com.quincy.auth.service;

import com.quincy.auth.o.User;
import com.quincy.auth.o.XSession;
import com.quincy.sdk.annotation.sharding.ShardingKey;

public interface XSessionServiceShardingProxy {
	public XSession create(@ShardingKey Long shardingKey, User user);
}