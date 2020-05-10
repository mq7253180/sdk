package com.quincy.auth.controller.oauth2;

import org.apache.oltu.oauth2.common.message.OAuthResponse.OAuthResponseBuilder;

import lombok.Data;

@Data
public class ValidationResult {
	private Integer errorStatus = null;
	private Integer errorResponse = null;
	private String error = null;
	private String redirectUri = null;
	private OAuthResponseBuilder builder = null;
}