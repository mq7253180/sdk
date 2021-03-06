package com.quincy.core.entity;

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
@Entity(name = "s_transaction_atomic")
public class TransactionAtomic {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	private Long id;
	@Column(name="tx_id")
	private Long txId;
	@Column(name="bean_name")
	private String beanName;
	@Column(name="method_name")
	private String methodName;//确认或撤消方法名
	@Column(name="status")
	private Integer status;//1执行成功; 0还未执行或执行失败
	@Column(name="sort")
	private Integer sort;
	@Column(name="msg")
	private String msg;
	@Transient
	private String confirmMethodName;
	@Transient
	private Object[] args;
	@Transient
	private Class<?>[] parameterTypes;
	@Transient
	private List<TransactionArg> argList;
}