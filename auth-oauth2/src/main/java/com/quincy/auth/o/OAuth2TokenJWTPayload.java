package com.quincy.auth.o;

import java.util.List;

import lombok.Data;

@Data
public class OAuth2TokenJWTPayload {
	private String clientId;
	private List<String> accounts;
	private List<String> scopes;
	private Long validBefore;
}