package com.quincy.core.entity;

import java.util.Date;
import java.util.List;

import com.quincy.sdk.annotation.jdbc.Column;
import com.quincy.sdk.annotation.jdbc.DTO;

@DTO
public class Transaction {
	@Column("id")
	private Long id;
	@Column("application_name")
	private String applicationName;
	@Column("bean_name")
	private String beanName;
	@Column("method_name")
	private String methodName;
	@Column("creation_time")
	private Date creationTime;
	@Column("last_executed")
	private Date lastExecuted;
	@Column("type")
	private Integer type;//0失败重试(定时任务执行status为0的原子操作); 1失败撤消(定时任务执行status为1的原子操作)
	@Column("status")
	private Integer status;//0正在执行; 1执行结束
	@Column("version")
	private Integer version;
	@Column("flag_for_cron_job")
	private String flagForCronJob;//频率批次名称
	@Column("in_order")
	private Boolean inOrder;//是否有顺序
	private Object[] args;
	private Class<?>[] parameterTypes;
	private List<TransactionAtomic> atomics;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getApplicationName() {
		return applicationName;
	}
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
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
	public Date getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}
	public Date getLastExecuted() {
		return lastExecuted;
	}
	public void setLastExecuted(Date lastExecuted) {
		this.lastExecuted = lastExecuted;
	}
	public Integer getType() {
		return type;
	}
	public void setType(Integer type) {
		this.type = type;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public Integer getVersion() {
		return version;
	}
	public void setVersion(Integer version) {
		this.version = version;
	}
	public String getFlagForCronJob() {
		return flagForCronJob;
	}
	public void setFlagForCronJob(String flagForCronJob) {
		this.flagForCronJob = flagForCronJob;
	}
	public Boolean getInOrder() {
		return inOrder;
	}
	public void setInOrder(Boolean inOrder) {
		this.inOrder = inOrder;
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
	public List<TransactionAtomic> getAtomics() {
		return atomics;
	}
	public void setAtomics(List<TransactionAtomic> atomics) {
		this.atomics = atomics;
	}
}