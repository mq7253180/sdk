package com.quincy.core.entity;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
	@Column(name="bean_name")
	private String beanName;
	@Column(name="method_name")
	private String methodName;
	@Column(name="last_executed_start")
	private Date lastExecutedStart;
	@Column(name="last_executed_end")
	private Date lastExecutedEnd;
	@Column(name="type")
	private Integer type;//0失败重试(定时任务执行status为0的原子操作); 1失败撤消(定时任务执行status为1的原子操作)
	@Column(name="status")
	private Integer status;//0正在执行; 1执行结束
	@Column(name="version")
	private Integer version;
	@Transient
	private Object[] args;
	@Transient
	private Class<?>[] parameterTypes;
	@Transient
	private List<TransactionAtomic> atomics;
}
