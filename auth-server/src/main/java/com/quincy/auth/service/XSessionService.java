package com.quincy.auth.service;

import com.quincy.auth.o.User;
import com.quincy.auth.o.XSession;

public interface XSessionService {
	public XSession create(User user);
}