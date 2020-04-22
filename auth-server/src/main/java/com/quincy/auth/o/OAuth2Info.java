package com.quincy.auth.o;

import java.util.List;

import lombok.Data;

@Data
public class OAuth2Info {
	private String id;
	private Long userId;
	private String clientId;
	private String authorizationCode;
	private List<String> scopes;
}