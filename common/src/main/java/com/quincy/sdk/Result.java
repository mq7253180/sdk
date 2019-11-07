package com.quincy.sdk;

public class Result {
	private int status;//1正常，0未登录，-1操作未被授权、被拒，-2签名验证失败，-3服务端程序抛异常，-4图片验证码过期，-5图片验证码输入有误，-6mobilephone字段为空，-7手机验证码过期，-8手机验证码输入有误
	private String msg;
	private Object data;
	private String cluster;

	public Result() {
		
	}
	public Result(int status, String msg, Object data) {
		this.status = status;
		this.msg = msg;
		this.data = data;
	}
	public Result(int status, String msg) {
		this(status, msg, null);
	}
	public Result(int status) {
		this(status, null, null);
	}

	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	public String getCluster() {
		return cluster;
	}
	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	public final static String I18N_KEY_SUCCESS = "status.success";
	public final static String I18N_KEY_EXCEPTION = "status.error.500";
	public final static String I18N_KEY_TIMEOUT = "status.error.401";
	public final static String I18N_KEY_DENY = "status.error.403";

	public static Result newSuccess() {
		return new Result(1, I18N_KEY_SUCCESS);
	}

	public static Result newException() {
		return new Result(-2, I18N_KEY_EXCEPTION);
	}

	public static Result newTimeout() {
		return new Result(0, I18N_KEY_TIMEOUT);
	}

	public static Result newDeny() {
		return new Result(-1, I18N_KEY_DENY);
	}

	public Result msg(String msg) {
		this.msg = msg;
		return this;
	}

	public Result data(Object data) {
		this.data = data;
		return this;
	}

	public Result cluster(String cluster) {
		this.cluster = cluster;
		return this;
	}
}
