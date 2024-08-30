package com.quincy.auth.service;

import com.quincy.auth.o.User;
import com.quincy.auth.o.XSession;

public interface XSessionServiceShardingProxy {
	public XSession create(long shardingKey, User user);
}