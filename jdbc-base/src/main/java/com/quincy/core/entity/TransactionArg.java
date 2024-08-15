package com.quincy.core.entity;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@DynamicInsert
@DynamicUpdate
@EntityListeners({AuditingEntityListener.class})
@Entity(name = "s_transaction_arg")
public class TransactionArg {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	private Long id;
	@Column(name="parent_id")
	private Long parentId;
	@Column(name="class")
	private String clazz;
	@Column(name="_value")
	private String value;
	@Column(name="sort")
	private Integer sort;
	@Column(name="type")
	private Integer type;
}
