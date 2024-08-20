package com.quincy.auth.service.impl;

import org.springframework.stereotype.Service;

import com.quincy.auth.o.User;
import com.quincy.auth.o.XSession;
import com.quincy.auth.service.XSessionServiceShardingProxy;
import com.quincy.sdk.annotation.ReadOnly;
import com.quincy.sdk.annotation.sharding.ShardingKey;

@Service
public class XSessionServiceShardingProxyImpl extends XSessionServiceImpl implements XSessionServiceShardingProxy {
	@ReadOnly
	@Override
	public XSession create(@ShardingKey Integer shardingKey, User user) {
		return this.create(user);
	}
}