package com.quincy.auth;

import lombok.Data;

@Data
public class OAuth2Result {
	private Integer errorStatus = null;
	private String errorUri = null;
	private String error = null;
	private Integer errorResponse = null;
}