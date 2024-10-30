package com.quincy.auth.service.impl;

import com.quincy.auth.service.XSessionServiceShardingProxy;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.annotation.sharding.ShardingKey;
import com.quincy.sdk.o.User;
import com.quincy.sdk.o.XSession;

public class XSessionServiceShardingProxyImpl extends XSessionServiceImpl implements XSessionServiceShardingProxy {
	@Override
	@ReadOnly
	public XSession create(@ShardingKey long shardingKey, User user) {
		return this.create(user);
	}
}