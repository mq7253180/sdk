package com.quincy.core.entity;

import java.util.Date;
import java.util.List;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Data;

@Data
@DynamicInsert
@DynamicUpdate
@EntityListeners({AuditingEntityListener.class})
@Entity(name = "s_transaction")
public class Transaction {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	private Long id;
	@Column(name="application_name")
	private String applicationName;
	@Column(name="bean_name")
	private String beanName;
	@Column(name="method_name")
	private String methodName;
	@Column(name="creation_time")
	private Date creationTime;
	@Column(name="last_executed")
	private Date lastExecuted;
	@Column(name="type")
	private Integer type;//0失败重试(定时任务执行status为0的原子操作); 1失败撤消(定时任务执行status为1的原子操作)
	@Column(name="status")
	private Integer status;//0正在执行; 1执行结束
	@Column(name="version")
	private Integer version;
	@Column(name="flag_for_cron_job")
	private String flagForCronJob;//频率批次名称
	@Column(name="in_order")
	private Boolean inOrder;//是否有顺序
	@Transient
	private Object[] args;
	@Transient
	private Class<?>[] parameterTypes;
	@Transient
	private List<TransactionAtomic> atomics;
}
