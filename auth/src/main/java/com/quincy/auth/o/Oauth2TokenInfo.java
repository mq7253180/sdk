package com.quincy.auth.o;

import java.util.List;

import lombok.Data;

@Data
public class Oauth2TokenInfo {
	private String clientId;
	private List<String> accounts;
	private List<String> scopes;
	private Long validBefore;
}