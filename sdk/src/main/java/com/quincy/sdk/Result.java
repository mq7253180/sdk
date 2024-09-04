package com.quincy.sdk;

import java.io.Serializable;

public class Result implements Serializable {
	private static final long serialVersionUID = 1L;
	/*
	 * 1成功, 0会话超时, -1无权限, -2签名传空, -3签名验证失败, -4抛异常, -5验证码输入为空，-6验证码过期, -7验证码输入有误, -8多租户模式下没有选择租户, -9邮箱找回密码时链接超时, -10邮箱找回密码时无效链接, -11找回密码时用户未注册
	 */
	private int status;
	private String msg;
	private Object data;
//	private Object accsessToken;

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
	/*public Object getAccsessToken() {
		return accsessToken;
	}
	public void setAccsessToken(Object accsessToken) {
		this.accsessToken = accsessToken;
	}*/

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
}