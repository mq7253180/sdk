package com.quincy.auth.service;

import com.quincy.auth.entity.ClientSystem;

public interface GeneralService {
	public ClientSystem findClientSystem(String clientId);
}