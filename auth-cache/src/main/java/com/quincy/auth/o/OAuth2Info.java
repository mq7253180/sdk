package com.quincy.auth.o;

import lombok.Data;

@Data
public class OAuth2Info {
	private Long clientSystemId;
	private Long userId;
	private String authorizationCode;
}