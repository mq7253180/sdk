package com.quincy.auth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;

import com.quincy.auth.service.XSessionServiceShardingProxy;
import com.quincy.sdk.o.User;
import com.quincy.sdk.o.XSession;
import com.quincy.auth.service.XSessionService;

@Primary
public class XSessionServiceShardingImpl implements XSessionService {
	@Autowired
	private XSessionServiceShardingProxy xSessionServiceShardingProxy;

	@Override
	public XSession create(User user) {
		return xSessionServiceShardingProxy.create(user.getShardingKey(), user);
	}
}