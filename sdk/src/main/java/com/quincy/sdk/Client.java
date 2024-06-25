package com.quincy.sdk;

import org.apache.http.entity.ContentType;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;

public enum Client {
	PC("pc", "p", null), Mobile("mobile", "m", null), 
	Ajax("ajax", "j", ContentType.APPLICATION_JSON.toString()), 
	Browser_Json("browser_json", "j", ContentType.APPLICATION_JSON.toString()), 
	Android("android", "j", ContentType.APPLICATION_JSON.toString()), 
	iOS("ios", "j", ContentType.APPLICATION_JSON.toString());
	
	private final static String[] MOBILE_USER_AGENT_FLAGS = {"iPhone", "iPad", "Android", "Symbian"};

	private String flag;
	private String suffix;
	private String contentType;
	private boolean json;
	private boolean app;

	private Client(String flag, String suffix, String contentType) {
		this.flag = flag;
		this.suffix = suffix;
		this.contentType = contentType;
		this.json = "j".equals(suffix);
		this.app = "android".equals(flag)||"ios".equals(flag);
	}

	public String getFlag() {
		return flag;
	}
	public String getSuffix() {
		return suffix;
	}
	public String getContentType() {
		return contentType;
	}
	public boolean isJson() {
		return json;
	}
	public boolean isApp() {
		return app;
	}

	public static Client get(HttpServletRequest request) {
		return get(request, null);
	}

	public static Client get(HttpServletRequest request, Object handler) {
		Client client = null;
		Object _client = request.getAttribute("client");
		if(_client!=null) {
			client = (Client)_client;
		} else {
			client = yes(request, handler);
			request.setAttribute("client", client);
		}
		return client;
	}

	private static Client yes(HttpServletRequest request, Object handler) {
		if("XMLHttpRequest".equals(request.getHeader("x-requested-with")))
			return Ajax;
		String userAgent = request.getHeader("user-agent");
		if(userAgent!=null) {
			for(String flag:MOBILE_USER_AGENT_FLAGS) {
				if(userAgent.contains(flag)) {
					return Mobile;
				}
			}
		}
//		if(true)
//			return iOS;
//		if(true)
//			return Android;
		if(handler!=null) {
			HandlerMethod method = (HandlerMethod)handler;
			ResponseBody annotation = method.getMethod().getDeclaredAnnotation(ResponseBody.class);
			if(annotation!=null)
				return Browser_Json;
		}
		return PC;
	}

	public static Client get(String flag) {
		for (Client c : Client.values()) { 
			if(c.getFlag().equals(flag))
				return c;
		}
		return null;
	}
}