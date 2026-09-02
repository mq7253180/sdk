package com.quincy.auth.service;

import com.quincy.auth.entity.UserDto;

public interface UserServiceShardingProxy {
	public Long findUserId(long loginNameHashCode, String loginName);
	public void add(long userId, UserDto vo);
	public Long createMapping(long loginNameHashCode, String loginName);
	public int deleteMapping(long loginNameHashCode, String loginName);
}