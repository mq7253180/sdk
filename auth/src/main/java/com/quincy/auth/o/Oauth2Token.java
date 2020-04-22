package com.quincy.auth.o;

import lombok.Data;

@Data
public class Oauth2Token {
	private Oauth2TokenInfo info;
	private String signature;
}