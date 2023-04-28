package com.quincy.sdk.helper;

public class SimplifiedHttpResponse {
	private String content;
	private String sessionId;

	public SimplifiedHttpResponse(String content, String sessionId) {
		this.content = content;
		this.sessionId = sessionId;
	}
	public String getContent() {
		return content;
	}
	public String getSessionId() {
		return sessionId;
	}
}
