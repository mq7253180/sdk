package com.quincy.auth.service.impl;

import org.springframework.stereotype.Service;

import com.quincy.auth.PermissionAndRoleConfiguration;
import com.quincy.auth.o.User;
import com.quincy.auth.o.XSession;
import com.quincy.auth.service.ShardingXSessionService;
import com.quincy.sdk.annotation.ReadOnly;
import com.quincy.sdk.annotation.sharding.ShardingKey;

@Service
public class ShardingXSessionServiceProxyImpl extends PermissionAndRoleConfiguration implements ShardingXSessionService {
	@ReadOnly
	@Override
	public XSession create(@ShardingKey Integer shardingKey, User user) {
		return this.create(user);
	}
}