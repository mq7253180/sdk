package com.quincy.core.entity;

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
	@Column(name="ret_class")
	private String retClass;
	@Column(name="ret_value")
	private String retValue;
	@Column(name="msg")
	private String msg;
	@Transient
	private String confirmMethodName;
	@Transient
	private Object[] args;
	@Transient
	private Class<?>[] parameterTypes;
	@Transient
	private String[] parameterTypeNames;
	@Transient
	private List<TransactionArg> argList;
	@Transient
	private Class<?> returnType;
}