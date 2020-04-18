package com.quincy.auth.service;

import com.quincy.auth.entity.ClientSystem;

public interface OAuth2Service {
	public ClientSystem findClientSystem(Long id);
	public ClientSystem findClientSystem(String clientId);
}