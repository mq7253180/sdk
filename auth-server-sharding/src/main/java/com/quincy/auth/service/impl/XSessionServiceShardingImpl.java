package com.quincy.auth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.quincy.auth.o.User;
import com.quincy.auth.o.XSession;
import com.quincy.auth.service.XSessionServiceShardingProxy;
import com.quincy.auth.service.XSessionService;

public class XSessionServiceShardingImpl implements XSessionService {
	@Autowired
	private XSessionServiceShardingProxy xSessionServiceShardingProxy;

	@Override
	public XSession create(User user) {
		return xSessionServiceShardingProxy.create(user.getShardingKey(), user);
	}
}