package com.quincy.auth.service.impl;

import com.quincy.auth.o.User;
import com.quincy.auth.o.XSession;
import com.quincy.auth.service.XSessionServiceShardingProxy;
import com.quincy.sdk.annotation.ReadOnly;
import com.quincy.sdk.annotation.sharding.ShardingKey;

public class XSessionServiceShardingProxyImpl extends XSessionServiceImpl implements XSessionServiceShardingProxy {
	@ReadOnly
	@Override
	public XSession create(@ShardingKey(snowFlake = true) Long shardingKey, User user) {
		return this.create(user);
	}
}