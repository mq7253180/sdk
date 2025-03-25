package com.quincy.sdk;

import org.apache.http.entity.ContentType;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;

public enum Client {
	PC_Page("pc_page", "p", null),
	PC_Ajax("pc_ajax", "j", ContentType.APPLICATION_JSON.toString()), 
	Mobile_Page("mobile_page", "m", null), 
	Mobile_Ajax("mobile_ajax", "j", ContentType.APPLICATION_JSON.toString()), 
	ResponseBody_Json("ResponseBody_json", "j", ContentType.APPLICATION_JSON.toString()),
	ContentType_Json("ContentType_json", "j", ContentType.APPLICATION_JSON.toString()),
	Android("android", "j", ContentType.APPLICATION_JSON.toString()), 
	iOS("ios", "j", ContentType.APPLICATION_JSON.toString());
	
	private final static String[] MOBILE_USER_AGENT_FLAGS = {"iPhone", "Android"};

	private String flag;
	private String suffix;
	private String contentType;
	private boolean json;
	private boolean pc;
	private boolean mobile;
	private boolean app;

	private Client(String flag, String suffix, String contentType) {
		this.flag = flag;
		this.suffix = suffix;
		this.contentType = contentType;
		this.json = "j".equals(suffix);
		this.pc = flag.startsWith("pc");
		this.mobile = flag.startsWith("mobile");
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
	public boolean isPc() {
		return pc;
	}
	public boolean isMobile() {
		return mobile;
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
//		if(true)
//			return iOS;
//		if(true)
//			return Android;
		String a = null;
		String userAgent = request.getHeader("user-agent");
		if(userAgent!=null) {
			for(String flag:MOBILE_USER_AGENT_FLAGS) {
				if(userAgent.contains(flag)) {
					a = "mobile";
					break;
				}
			}
		}
		if(a==null)
			a = "pc";
		String b = null;
		if("XMLHttpRequest".equals(request.getHeader("x-requested-with"))) {
			b = "ajax";
		} else {
			b = "page";
			if(handler!=null) {
				HandlerMethod method = (HandlerMethod)handler;
				ResponseBody annotation = method.getMethod().getDeclaredAnnotation(ResponseBody.class);
				if(annotation!=null)
					return ResponseBody_Json;
			}
			if(ContentType.APPLICATION_JSON.toString().indexOf(request.getContentType())>=0)
				return ContentType_Json;
		}
		String flag = a+"_"+b;
		for (Client c : Client.values()) { 
			if(c.getFlag().equals(flag))
				return c;
		}
		return null;
	}
}