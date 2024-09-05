package com.quincy.auth.service;

import com.quincy.sdk.o.User;
import com.quincy.sdk.o.XSession;

public interface XSessionService {
	public XSession create(User user);
}