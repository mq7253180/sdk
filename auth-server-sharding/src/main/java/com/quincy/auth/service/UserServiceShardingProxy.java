package com.quincy.auth.service;

import com.quincy.auth.entity.UserDto;
import com.quincy.sdk.Client;
import com.quincy.sdk.o.User;

public interface UserServiceShardingProxy {
	public void loadAuth(long shardingKey, User user);
	public UserDto update(long shardingKey, UserDto vo);
	public Long findUserId(long shardingKey, String loginName);
	public User find(long shardingKey, Long id, Client client);
	public void updatePassword(Long id, String password);
	public void add(long shardingKey, UserDto vo);
	public boolean createMapping(long shardingKey, String loginName, Long userId);
	public int deleteMapping(long shardingKey, String loginName);
	public int updateJsessionidPcBrowser(Long id, String jsessionid);
	public int updateJsessionidMobileBrowser(Long id, String jsessionid);
	public int updateJsessionidApp(Long id, String jsessionid);
}