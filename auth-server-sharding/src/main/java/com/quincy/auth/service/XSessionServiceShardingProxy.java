package com.quincy.auth.service;

import com.quincy.sdk.o.User;
import com.quincy.sdk.o.XSession;

public interface XSessionServiceShardingProxy {
	public XSession create(long shardingKey, User user);
}