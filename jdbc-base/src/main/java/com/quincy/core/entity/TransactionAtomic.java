package com.quincy.core.entity;

import java.util.List;

import com.quincy.sdk.annotation.jdbc.Column;
import com.quincy.sdk.annotation.jdbc.DTO;

@DTO
public class TransactionAtomic {
	@Column("id")
	private Long id;
	@Column("tx_id")
	private Long txId;
	@Column("bean_name")
	private String beanName;
	@Column("method_name")
	private String methodName;//确认或撤消方法名
	@Column("status")
	private Integer status;//1执行成功; 0还未执行或执行失败
	@Column("sort")
	private Integer sort;
	@Column("ret_class")
	private String retClass;
	@Column("ret_value")
	private String retValue;
	@Column("msg")
	private String msg;
	private String confirmMethodName;
	private Object[] args;
	private Class<?>[] parameterTypes;
	private String[] parameterTypeNames;
	private List<TransactionArg> argList;
	private Class<?> returnType;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getTxId() {
		return txId;
	}
	public void setTxId(Long txId) {
		this.txId = txId;
	}
	public String getBeanName() {
		return beanName;
	}
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public Integer getSort() {
		return sort;
	}
	public void setSort(Integer sort) {
		this.sort = sort;
	}
	public String getRetClass() {
		return retClass;
	}
	public void setRetClass(String retClass) {
		this.retClass = retClass;
	}
	public String getRetValue() {
		return retValue;
	}
	public void setRetValue(String retValue) {
		this.retValue = retValue;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public String getConfirmMethodName() {
		return confirmMethodName;
	}
	public void setConfirmMethodName(String confirmMethodName) {
		this.confirmMethodName = confirmMethodName;
	}
	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}
	public void setParameterTypes(Class<?>[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}
	public String[] getParameterTypeNames() {
		return parameterTypeNames;
	}
	public void setParameterTypeNames(String[] parameterTypeNames) {
		this.parameterTypeNames = parameterTypeNames;
	}
	public List<TransactionArg> getArgList() {
		return argList;
	}
	public void setArgList(List<TransactionArg> argList) {
		this.argList = argList;
	}
	public Class<?> getReturnType() {
		return returnType;
	}
	public void setReturnType(Class<?> returnType) {
		this.returnType = returnType;
	}
}