package com.quincy.auth.service;

public interface UserShardingService {
	public void deleteMappingAndUpdateUser(String oldLoginName, UserUpdation userUpdation, Long userId);
}